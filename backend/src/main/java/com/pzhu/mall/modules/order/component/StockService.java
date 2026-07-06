package com.pzhu.mall.modules.order.component;

import com.pzhu.mall.common.config.RedisKeyPrefix;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.modules.order.entity.Order;
import com.pzhu.mall.modules.order.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Redis 库存预扣减与归还服务。
 */
@Component
public class StockService {

    @Resource
    private StringRedisTemplate redisTemplate;

    @Resource
    private OrderMapper orderMapper;

    @Value("${mall.stock.lock-wait-seconds:3}")
    private long lockWaitSeconds;

    @Value("${mall.stock.lock-lease-seconds:10}")
    private long lockLeaseSeconds;

    private String stockKey(Long skuId) {
        return RedisKeyPrefix.STOCK + ":" + skuId;
    }

    private String lockKey(Long skuId) {
        return RedisKeyPrefix.STOCK_LOCK + ":" + skuId;
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
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", lockLeaseSeconds, TimeUnit.SECONDS);
        if (locked == null || !locked) {
            throw new BusinessException(ErrorCode.SYSTEM_BUSY);
        }
        try {
            // 懒加载：Redis 中无此 key 时从数据库加载
            if (!redisTemplate.hasKey(stockKey)) {
                // TODO: 从数据库 sku.stock 加载并写入（当前阶段先放行）
            }
            Long stock = redisTemplate.opsForValue().increment(stockKey, -quantity);
            // increment 返回值是新值，如果之前为 null 则返回 -quantity（实际不存在的情况已由 hasKey 处理）
            // 重新获取当前值判断
            String currentStr = redisTemplate.opsForValue().get(stockKey);
            long current = currentStr != null ? Long.parseLong(currentStr) : 0;
            if (current < 0) {
                // 库存不足，回滚
                redisTemplate.opsForValue().increment(stockKey, quantity);
                return false;
            }
            return true;
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    /**
     * 归还预扣减的库存。
     */
    public void rollback(Long skuId, int quantity) {
        String stockKey = stockKey(skuId);
        redisTemplate.opsForValue().increment(stockKey, quantity);
    }
}
