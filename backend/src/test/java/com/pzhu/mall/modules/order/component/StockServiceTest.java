package com.pzhu.mall.modules.order.component;

import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.modules.product.entity.Product;
import com.pzhu.mall.modules.product.entity.Sku;
import com.pzhu.mall.modules.product.mapper.ProductMapper;
import com.pzhu.mall.modules.product.mapper.SkuMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * StockService 单元测试（防超卖核心：Redis 预扣减 + 懒加载 + 归还）。
 * <p>覆盖 docs/32 批次1 的 O-T01~06 用例：库存充足/不足/懒加载/回滚/锁竞争/低库存预警。</p>
 */
class StockServiceTest {

    private RedissonClient redissonClient;
    private StringRedisTemplate stringRedisTemplate;
    private ValueOperations<String, String> valueOperations;
    private SkuMapper skuMapper;
    private ProductMapper productMapper;
    private RLock lock;
    private StockService service;

    @BeforeEach
    void setUp() throws InterruptedException {
        redissonClient = mock(RedissonClient.class);
        stringRedisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        skuMapper = mock(SkuMapper.class);
        productMapper = mock(ProductMapper.class);
        lock = mock(RLock.class);
        service = new StockService();
        inject(service, "redissonClient", redissonClient);
        inject(service, "stringRedisTemplate", stringRedisTemplate);
        inject(service, "skuMapper", skuMapper);
        inject(service, "productMapper", productMapper);
        inject(service, "lockWaitSeconds", 3L);
        inject(service, "lockLeaseSeconds", 10L);
        inject(service, "alertThreshold", 10);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
    }

    // ==================== deduct（SKU 维度） ====================

    @Test
    void deduct_stockSufficient_returnsTrue() {
        // Redis 已有库存 100，扣 1 → 剩余 99
        when(stringRedisTemplate.hasKey("mall:stock:1")).thenReturn(true);
        when(stringRedisTemplate.execute(any(), anyList(), anyString(), anyString()))
                .thenReturn(99L);

        boolean ok = service.deduct(1L, 1);

        assertTrue(ok);
        // 懒加载不应触发（key 已存在）
        verify(skuMapper, never()).selectById(anyLong());
    }

    @Test
    void deduct_stockInsufficient_returnsFalse() {
        // Redis 库存 5，扣 10 → Lua 返回 -1
        when(stringRedisTemplate.hasKey("mall:stock:1")).thenReturn(true);
        when(stringRedisTemplate.execute(any(), anyList(), anyString(), anyString()))
                .thenReturn(-1L);

        boolean ok = service.deduct(1L, 10);

        assertFalse(ok);
    }

    @Test
    void deduct_keyAbsent_loadsFallbackFromSku() {
        // Redis 无 key → 从 DB 懒加载 sku.stock=50，扣 3
        Sku sku = new Sku();
        sku.setId(1L);
        sku.setStock(50);
        when(stringRedisTemplate.hasKey("mall:stock:1")).thenReturn(false);
        when(skuMapper.selectById(1L)).thenReturn(sku);
        when(stringRedisTemplate.execute(any(), anyList(), anyString(), anyString()))
                .thenReturn(47L);

        boolean ok = service.deduct(1L, 3);

        assertTrue(ok);
        verify(skuMapper).selectById(1L);
        // 扣减数量与懒加载初始值作为参数传入 Lua
        verify(stringRedisTemplate).execute(any(), eq(Collections.singletonList("mall:stock:1")),
                eq("3"), eq("50"));
    }

    @Test
    void deduct_keyAbsent_skuNotExist_fallbackZero() {
        // SKU 不存在 → fallback=0 → Lua 返回 -1 → false
        when(stringRedisTemplate.hasKey("mall:stock:1")).thenReturn(false);
        when(skuMapper.selectById(1L)).thenReturn(null);
        when(stringRedisTemplate.execute(any(), anyList(), anyString(), anyString()))
                .thenReturn(-1L);

        boolean ok = service.deduct(1L, 1);

        assertFalse(ok);
        verify(stringRedisTemplate).execute(any(), anyList(), eq("1"), eq("0"));
    }

    @Test
    void deduct_executeReturnsNull_returnsFalse() {
        // SB-02 防御路径：Redis execute 返回 null → 按失败处理
        when(stringRedisTemplate.hasKey("mall:stock:1")).thenReturn(true);
        when(stringRedisTemplate.execute(any(), anyList(), anyString(), anyString()))
                .thenReturn(null);

        boolean ok = service.deduct(1L, 1);

        assertFalse(ok);
    }

    @Test
    void deduct_lowStock_alertLogged() {
        // 剩余 8 ≤ threshold 10 → 记低库存警告（返回仍 true）
        when(stringRedisTemplate.hasKey("mall:stock:1")).thenReturn(true);
        when(stringRedisTemplate.execute(any(), anyList(), anyString(), anyString()))
                .thenReturn(8L);

        boolean ok = service.deduct(1L, 1);

        assertTrue(ok);
    }

    @Test
    void deduct_lockNotAcquired_throwsSystemBusy() throws InterruptedException {
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.deduct(1L, 1));
        assertEquals(ErrorCode.SYSTEM_BUSY.getCode(), ex.getCode());
    }

    @Test
    void deduct_lockInterrupted_throwsSystemBusy() throws InterruptedException {
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenThrow(new InterruptedException("interrupt"));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.deduct(1L, 1));
        assertEquals(ErrorCode.SYSTEM_BUSY.getCode(), ex.getCode());
        // 中断标志复位
        assertTrue(Thread.interrupted());
    }

    // ==================== deductProduct（商品维度） ====================

    @Test
    void deductProduct_stockSufficient_returnsTrue() {
        when(stringRedisTemplate.hasKey("mall:stock:product:10")).thenReturn(true);
        when(stringRedisTemplate.execute(any(), anyList(), anyString(), anyString()))
                .thenReturn(9L);

        boolean ok = service.deductProduct(10L, 1);

        assertTrue(ok);
        verify(productMapper, never()).selectById(anyLong());
    }

    @Test
    void deductProduct_keyAbsent_loadsFromProduct() {
        Product p = new Product();
        p.setId(10L);
        p.setStock(20);
        when(stringRedisTemplate.hasKey("mall:stock:product:10")).thenReturn(false);
        when(productMapper.selectById(10L)).thenReturn(p);
        when(stringRedisTemplate.execute(any(), anyList(), anyString(), anyString()))
                .thenReturn(19L);

        boolean ok = service.deductProduct(10L, 1);

        assertTrue(ok);
        verify(productMapper).selectById(10L);
        verify(stringRedisTemplate).execute(any(), eq(Collections.singletonList("mall:stock:product:10")),
                eq("1"), eq("20"));
    }

    // ==================== rollback / rollbackProduct ====================

    @Test
    void rollback_incrementsRedis() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        service.rollback(1L, 5);

        verify(valueOperations).increment("mall:stock:1", 5);
        verify(lock).lock();
    }

    @Test
    void rollback_zeroQuantity_skips() {
        service.rollback(1L, 0);

        verify(stringRedisTemplate, never()).opsForValue();
        verify(lock, never()).lock();
    }

    @Test
    void rollbackProduct_incrementsProductKey() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        service.rollbackProduct(10L, 3);

        verify(valueOperations).increment("mall:stock:product:10", 3);
    }

    @Test
    void rollback_quantityNegative_skips() {
        service.rollback(1L, -1);

        verify(stringRedisTemplate, never()).opsForValue();
    }

    // ==================== 锁释放 ====================

    @Test
    void deduct_releasesLockInFinally() {
        when(stringRedisTemplate.hasKey("mall:stock:1")).thenReturn(true);
        when(stringRedisTemplate.execute(any(), anyList(), anyString(), anyString()))
                .thenReturn(99L);

        service.deduct(1L, 1);

        verify(lock).unlock();
    }

    @Test
    void deduct_insufficient_releasesLock() {
        when(stringRedisTemplate.hasKey("mall:stock:1")).thenReturn(true);
        when(stringRedisTemplate.execute(any(), anyList(), anyString(), anyString()))
                .thenReturn(-1L);

        service.deduct(1L, 10);

        verify(lock).unlock();
    }

    private static void inject(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
