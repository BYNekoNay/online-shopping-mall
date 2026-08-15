package com.pzhu.mall.modules.order.component;

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
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * IT-T05 并发防超卖测试（服务层，可离线执行）。
 *
 * <p>用 AtomicInteger 模拟 Redis Lua 脚本的原子扣减语义（等价于 Redis 单线程
 * 执行 DEDUCT_STOCK_SCRIPT），50 线程并发扣减同一 SKU：
 * 库存 10 时恰好 10 次成功、40 次返回 false（零超卖）。</p>
 */
class ConcurrencyTest {

    private RedissonClient redissonClient;
    private StringRedisTemplate stringRedisTemplate;
    private ValueOperations<String, String> valueOperations;
    private SkuMapper skuMapper;
    private ProductMapper productMapper;
    private RLock lock;
    private StockService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
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

    @Test
    void deduct_concurrent50_stock10_zeroOversell() throws Exception {
        // 模拟 Redis 初始库存 10（DB sku.stock 兜底加载值）
        AtomicInteger redisStock = new AtomicInteger(10);
        AtomicBoolean lazyLoaded = new AtomicBoolean(false);

        Sku sku = new Sku();
        sku.setId(1L);
        sku.setProductId(100L);
        sku.setStock(10);
        when(skuMapper.selectById(1L)).thenReturn(sku);

        // 第一次 hasKey=false 触发懒加载（fallback=DB stock），之后 key 存在
        when(stringRedisTemplate.hasKey(anyString())).thenAnswer(inv -> lazyLoaded.get());
        when(stringRedisTemplate.execute(any(org.springframework.data.redis.core.script.RedisScript.class),
                anyList(), any(), any())).thenAnswer(inv -> {
            lazyLoaded.set(true);
            // 等价 Lua 原子语义：先比较，不足直接返回 -1（不修改库存），足够才扣减
            int qty = Integer.parseInt(inv.getArgument(2, String.class));
            synchronized (redisStock) {
                int cur = redisStock.get();
                if (cur < qty) {
                    return -1L;
                }
                redisStock.set(cur - qty);
                return (long) (cur - qty);
            }
        });

        int threads = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger fail = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    if (service.deduct(1L, 1)) {
                        success.incrementAndGet();
                    } else {
                        fail.incrementAndGet();
                    }
                } catch (Exception e) {
                    fail.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        start.countDown();
        done.await(60, TimeUnit.SECONDS);
        pool.shutdown();

        // 零超卖：恰好 10 成功、40 失败
        assertEquals(10, success.get(), "库存 10 恰好 10 次扣减成功（零超卖）");
        assertEquals(40, fail.get(), "其余 40 次必须返回 false（库存不足）");
        assertEquals(0, redisStock.get(), "库存必须恰好扣完，不得为负");
    }

    @Test
    void deduct_concurrent20_overDemand200_only9Succeed() throws Exception {
        // 20 线程 × 每单 10 件，库存 99 → 恰好 9 单成功（90 件），11 单失败
        AtomicInteger redisStock = new AtomicInteger(99);
        AtomicBoolean lazyLoaded = new AtomicBoolean(false);

        Sku sku = new Sku();
        sku.setId(2L);
        sku.setProductId(200L);
        sku.setStock(99);
        when(skuMapper.selectById(2L)).thenReturn(sku);

        when(stringRedisTemplate.hasKey(anyString())).thenAnswer(inv -> lazyLoaded.get());
        when(stringRedisTemplate.execute(any(org.springframework.data.redis.core.script.RedisScript.class),
                anyList(), any(), any())).thenAnswer(inv -> {
            lazyLoaded.set(true);
            // 等价 Lua 原子语义：先比较，不足直接返回 -1（不修改库存），足够才扣减
            int qty = Integer.parseInt(inv.getArgument(2, String.class));
            synchronized (redisStock) {
                int cur = redisStock.get();
                if (cur < qty) {
                    return -1L;
                }
                redisStock.set(cur - qty);
                return (long) (cur - qty);
            }
        });

        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    if (service.deduct(2L, 10)) {
                        success.incrementAndGet();
                    }
                } catch (Exception ignored) {
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        start.countDown();
        done.await(60, TimeUnit.SECONDS);
        pool.shutdown();

        // 99/10 = 9 单成功（90 件），第 10 单需求 10 > 剩余 9 → 失败
        assertEquals(9, success.get(), "99 件库存恰好支持 9 单×10 件（零超卖）");
        assertEquals(9, redisStock.get(), "剩余 9 件");
    }

    // ==================== helpers ====================

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
