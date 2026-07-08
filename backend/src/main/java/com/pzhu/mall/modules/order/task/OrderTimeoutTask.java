package com.pzhu.mall.modules.order.task;

import com.pzhu.mall.common.config.RedisKeyPrefix;
import com.pzhu.mall.modules.order.entity.Order;
import com.pzhu.mall.modules.order.mapper.OrderMapper;
import com.pzhu.mall.modules.order.service.OrderService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 订单超时自动取消定时任务。
 */
@Component
public class OrderTimeoutTask {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutTask.class);

    /** 防止重复取消的锁过期时间（秒），超过此时间锁自动释放，允许重试 */
    private static final long CANCEL_LOCK_TTL_SECONDS = 120;

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private com.pzhu.mall.modules.order.service.OrderService orderService;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Value("${mall.order.timeout-minutes:30}")
    private int timeoutMinutes;

    @Scheduled(cron = "0 * * * * ?")
    public void cancelTimeoutOrders() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(timeoutMinutes);
        List<Order> timeoutOrders = orderMapper.selectTimeoutUnpaidOrders(timeoutMinutes, threshold);
        for (Order order : timeoutOrders) {
            Long orderId = order.getId();
            // 使用 Redis SET NX 作幂等标记，避免同一订单被多个实例重复取消
            String cancelKey = RedisKeyPrefix.ORDER + ":cancel:lock:" + orderId;
            Boolean alreadyCancelling = stringRedisTemplate.opsForValue()
                    .setIfAbsent(cancelKey, "1", CANCEL_LOCK_TTL_SECONDS, TimeUnit.SECONDS);
            if (alreadyCancelling == null || !alreadyCancelling) {
                // 其他实例正在处理或已完成，跳过
                continue;
            }
            try {
                orderService.cancelOrder(orderId);
            } catch (Exception e) {
                log.error("Failed to cancel timeout order: {}", orderId, e);
            }
            // 不主动删除 cancelKey (TTL 到期自动释放，防止任务异常中断后锁永久持有)
        }
    }
}
