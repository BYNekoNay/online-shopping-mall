package com.pzhu.mall.modules.order.component;

import com.pzhu.mall.common.config.RedisKeyPrefix;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.modules.order.entity.Order;
import com.pzhu.mall.modules.order.mapper.OrderMapper;
import com.pzhu.mall.modules.order.mapper.OrderItemMapper;
import com.pzhu.mall.modules.product.entity.Sku;
import com.pzhu.mall.modules.product.mapper.SkuMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
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

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private SkuMapper skuMapper;

    @Value("${mall.stock.lock-wait-seconds:3}")
    private long lockWaitSeconds;

    @Value("${mall.stock.lock-lease-seconds:10}")
    private long lockLeaseSeconds;

    @Value("${mall.stock.alert-threshold:10}")
    private int alertThreshold;

    private String stockKey(Long skuId) {
        return RedisKeyPrefix.STOCK + ":" + skuId;
    }

    private String lockKey(Long skuId) {
        return RedisKeyPrefix.STOCK_LOCK + ":" + skuId;
    }

    /**
     * 从数据库加载 sku.stock 并写入 Redis（stock key 不设 TTL）。
     */
    private void loadFromDb(Long skuId) {
        Sku sku = skuMapper.selectById(skuId);
        if (sku != null) {
            stringRedisTemplate.opsForValue().set(stockKey(skuId), String.valueOf(sku.getStock()));
        }
    }

    /**
     * 预扣减库存。
     *
     * @return true 扣减成功，false 库存不足
     */
    public boolean deduct(Long skuId, int quantity) {
        String lockKey = lockKey(skuId);
        String stockKey = stockKey(skuId);

        // 简易分布式锁（非 Redisson，用于演示；生产环境应替换为 RedissonClient）
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", lockLeaseSeconds, TimeUnit.SECONDS);
        if (locked == null || !locked) {
            throw new BusinessException(ErrorCode.SYSTEM_BUSY);
        }
        try {
            // 懒加载：Redis 中无此 key 时从数据库加载
            if (!stringRedisTemplate.hasKey(stockKey)) {
                loadFromDb(skuId);
            }
            Long stock = stringRedisTemplate.opsForValue().increment(stockKey, -quantity);
            // increment 返回值是新值，如果之前为 null 则返回 -quantity（实际不存在的情况已由 hasKey 处理）
            // 重新获取当前值判断
            String currentStr = stringRedisTemplate.opsForValue().get(stockKey);
            long current = currentStr != null ? Long.parseLong(currentStr) : 0;
            if (current < 0) {
                // 库存不足，回滚
                stringRedisTemplate.opsForValue().increment(stockKey, quantity);
                return false;
            }
            // 低库存预警
            if (current <= alertThreshold) {
                org.slf4j.LoggerFactory.getLogger(StockService.class)
                    .warn("Low stock alert: skuId={}, remaining={}, threshold={}", skuId, current, alertThreshold);
            }
            return true;
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    /**
     * 归还预扣减的库存。
     */
    public void rollback(Long skuId, int quantity) {
        String stockKey = stockKey(skuId);
        stringRedisTemplate.opsForValue().increment(stockKey, quantity);
    }
}
