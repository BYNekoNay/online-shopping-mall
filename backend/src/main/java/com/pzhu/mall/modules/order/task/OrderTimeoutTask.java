package com.pzhu.mall.modules.order.task;

import com.pzhu.mall.modules.order.entity.Order;
import com.pzhu.mall.modules.order.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 订单超时自动取消定时任务。
 */
@Component
public class OrderTimeoutTask {

    @Resource
    private OrderMapper orderMapper;

    @Value("${mall.order.timeout-minutes:30}")
    private int timeoutMinutes;

    @Scheduled(cron = "0 * * * * ?")
    public void cancelTimeoutOrders() {
        java.time.LocalDateTime threshold = java.time.LocalDateTime.now().minusMinutes(timeoutMinutes);
        // 简单查询：待付款且创建时间超过超时时间
        var qw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order>();
        qw.eq(Order::getStatus, 0)
          .lt(Order::getCreateTime, threshold)
          .last("LIMIT 100");
        // TODO: 调用 OrderService.cancelOrder 取消订单并归还库存
    }
}
