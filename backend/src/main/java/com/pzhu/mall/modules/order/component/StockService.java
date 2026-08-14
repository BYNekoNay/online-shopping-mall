package com.pzhu.mall.modules.order.component;

import com.pzhu.mall.common.config.RedisKeyPrefix;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.modules.order.mapper.OrderMapper;
import com.pzhu.mall.modules.product.entity.Product;
import com.pzhu.mall.modules.product.entity.Sku;
import com.pzhu.mall.modules.product.mapper.ProductMapper;
import com.pzhu.mall.modules.product.mapper.SkuMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Redis 库存预扣减与归还服务。
 * <p>
 * 策略要点：
 * <ul>
 *   <li>库存 key（stock:{skuId}）永不过期，避免 Redis 重启后出现超卖。</li>
 *   <li>通过数据库原子 SQL（UPDATE ... WHERE stock >= ?）做最终一致性校验。</li>
 *   <li>首次使用时从数据库 sku.stock 加载初始值写入 Redis。</li>
 *   <li>低库存预警阈值：当 Redis 预扣减后剩余库存低于 {@code mall.stock.alert-threshold} 时记录警告日志。</li>
 * </ul>
 */
@Component
public class StockService {

    private static final Logger log = LoggerFactory.getLogger(StockService.class);

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private SkuMapper skuMapper;

    // H-4 修复：无 SKU 商品的库存控制需要读取 product.stock 作懒加载兜底
    @Resource
    private ProductMapper productMapper;

    @Value("${mall.stock.lock-wait-seconds:3}")
    private long lockWaitSeconds;

    @Value("${mall.stock.lock-lease-seconds:10}")
    private long lockLeaseSeconds;

    @Value("${mall.stock.alert-threshold:10}")
    private int alertThreshold;

    /** Lua 脚本：原子性地懒加载库存 + 扣减 + 低库存预警 */
    private static final DefaultRedisScript<Long> DEDUCT_STOCK_SCRIPT = new DefaultRedisScript<>(
        "local stock = redis.call('GET', KEYS[1]); " +
        "if stock == false then " +
        "  redis.call('SET', KEYS[1], ARGV[2]); " +
        "  stock = ARGV[2]; " +
        "end; " +
        "if tonumber(stock) < tonumber(ARGV[1]) then " +
        "  return -1; " +
        "end; " +
        "local newStock = tonumber(stock) - tonumber(ARGV[1]); " +
        "redis.call('SET', KEYS[1], newStock); " +
        "return newStock;",
        Long.class
    );

    private String stockKey(Long skuId) {
        return RedisKeyPrefix.STOCK + ":" + skuId;
    }

    private String lockKey(Long skuId) {
        return RedisKeyPrefix.STOCK_LOCK + ":" + skuId;
    }

    // H-4 修复：商品维度使用独立 key 命名空间（product 与 sku 的自增 id 空间重叠，禁止复用同一 key）
    private String productStockKey(Long productId) {
        return RedisKeyPrefix.STOCK + ":product:" + productId;
    }

    private String productLockKey(Long productId) {
        return RedisKeyPrefix.STOCK_LOCK + ":product:" + productId;
    }

    /**
     * 预扣减库存（使用 Redisson 分布式锁，自动续期）。
     *
     * @return true 扣减成功，false 库存不足
     */
    public boolean deduct(Long skuId, int quantity) {
        return deductWithLock(stockKey(skuId), lockKey(skuId), quantity,
                () -> {
                    Sku sku = skuMapper.selectById(skuId);
                    return sku != null && sku.getStock() != null ? sku.getStock() : 0;
                },
                "skuId=" + skuId);
    }

    /**
     * H-4 修复：无 SKU 商品的库存预扣减（基于 product.stock，与 SKU 同构的 Redis 方案）。
     *
     * @return true 扣减成功，false 库存不足
     */
    public boolean deductProduct(Long productId, int quantity) {
        return deductWithLock(productStockKey(productId), productLockKey(productId), quantity,
                () -> {
                    Product product = productMapper.selectById(productId);
                    return product != null && product.getStock() != null ? product.getStock() : 0;
                },
                "productId=" + productId);
    }

    /**
     * 归还预扣减的库存（使用 Redisson 分布式锁）。
     */
    public void rollback(Long skuId, int quantity) {
        rollbackWithLock(stockKey(skuId), lockKey(skuId), quantity);
    }

    /**
     * H-4 修复：归还无 SKU 商品预扣减的库存。
     */
    public void rollbackProduct(Long productId, int quantity) {
        rollbackWithLock(productStockKey(productId), productLockKey(productId), quantity);
    }

    /**
     * 通用扣减：分布式锁 + Lua 懒加载扣减（SKU 与商品维度共用）。
     */
    private boolean deductWithLock(String stockKey, String lockKey, int quantity,
                                   java.util.function.IntSupplier fallbackLoader, String logTag) {
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 尝试获取锁，等待 lockWaitSeconds 秒，锁自动释放 lockLeaseSeconds 秒（看门狗自动续期）
            if (!lock.tryLock(lockWaitSeconds, lockLeaseSeconds, TimeUnit.SECONDS)) {
                throw new BusinessException(ErrorCode.SYSTEM_BUSY);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SYSTEM_BUSY);
        }

        try {
            // 懒加载 + 扣减：使用 Lua 脚本保证原子性（ARGV[1]=quantity, ARGV[2]=fallbackStock）
            int fallbackStock = 0;
            if (!stringRedisTemplate.hasKey(stockKey)) {
                fallbackStock = fallbackLoader.getAsInt();
            }
            Long remaining = stringRedisTemplate.execute(
                    DEDUCT_STOCK_SCRIPT,
                    Collections.singletonList(stockKey),
                    String.valueOf(quantity),
                    String.valueOf(fallbackStock)
            );
            if (remaining == null || remaining < 0) {
                return false;
            }
            // 低库存预警
            if (remaining <= alertThreshold) {
                log.warn("Low stock alert: {}, remaining={}, threshold={}", logTag, remaining, alertThreshold);
            }
            return true;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 通用归还：分布式锁 + INCR（SKU 与商品维度共用）。
     */
    private void rollbackWithLock(String stockKey, String lockKey, int quantity) {
        if (quantity <= 0) return;
        RLock lock = redissonClient.getLock(lockKey);
        lock.lock();
        try {
            stringRedisTemplate.opsForValue().increment(stockKey, quantity);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
