package com.pzhu.mall.modules.order.task;

import com.pzhu.mall.modules.order.entity.Order;
import com.pzhu.mall.modules.order.mapper.OrderMapper;
import com.pzhu.mall.modules.order.service.OrderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单超时自动取消定时任务。
 */
@Component
public class OrderTimeoutTask {

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private com.pzhu.mall.modules.order.service.OrderService orderService;

    @Value("${mall.order.timeout-minutes:30}")
    private int timeoutMinutes;

    @Scheduled(cron = "0 * * * * ?")
    public void cancelTimeoutOrders() {
        java.time.LocalDateTime threshold = java.time.LocalDateTime.now().minusMinutes(timeoutMinutes);
        List<Order> timeoutOrders = orderMapper.selectTimeoutUnpaidOrders(timeoutMinutes, threshold);
        for (Order order : timeoutOrders) {
            try {
                orderService.cancelOrder(order.getId());
            } catch (Exception e) {
                // 记录日志但不中断其他订单的取消
                org.slf4j.LoggerFactory.getLogger(OrderTimeoutTask.class)
                    .error("Failed to cancel timeout order: {}", order.getId(), e);
            }
        }
    }
}
