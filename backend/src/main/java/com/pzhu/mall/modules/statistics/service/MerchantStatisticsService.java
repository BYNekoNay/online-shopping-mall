package com.pzhu.mall.modules.statistics.service;

import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.modules.order.entity.Order;
import com.pzhu.mall.modules.order.entity.OrderItem;
import com.pzhu.mall.modules.order.mapper.OrderItemMapper;
import com.pzhu.mall.modules.order.mapper.OrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 商家统计数据统计服务。
 */
@Service
public class MerchantStatisticsService {

    private static final Logger log = LoggerFactory.getLogger(MerchantStatisticsService.class);

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private OrderItemMapper orderItemMapper;

    /**
     * 销售统计（按日/周/月聚合）。
     *
     * @param shopId      商家ID
     * @param startDate   开始日期
     * @param endDate     结束日期
     * @param granularity day / week / month
     * @return { totalAmount, totalOrders, trend: [{date, amount, orders}] }
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getSalesStatistics(Long shopId, LocalDate startDate, LocalDate endDate, String granularity) {
        log.info("[商家统计] shopId={}, range={}~{} , granularity={}", shopId, startDate, endDate, granularity);

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        // 使用 SQL 聚合：统计订单数和总金额（排除赠品行在内存中处理）
        var qw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order>()
                .eq(Order::getShopId, shopId)
                .eq(Order::getIsDeleted, 0)
                .ge(Order::getCreateTime, start)
                .le(Order::getCreateTime, end)
                .orderByAsc(Order::getCreateTime);
        List<Order> orders = orderMapper.selectList(qw);

        int totalOrders = orders.size();
        Map<String, BigDecimal> amountByBucket = new LinkedHashMap<>();
        Map<String, Integer> ordersByBucket = new LinkedHashMap<>();

        // 批量加载所有订单的商品明细（减少 N+1 查询）
        if (!orders.isEmpty()) {
            List<Long> orderIds = orders.stream().map(Order::getId).collect(java.util.stream.Collectors.toList());
            List<OrderItem> allItems = orderItemMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrderItem>()
                            .in(OrderItem::getOrderId, orderIds)
            );
            Map<Long, List<OrderItem>> itemsByOrder = allItems.stream()
                    .collect(java.util.stream.Collectors.groupingBy(OrderItem::getOrderId));

            BigDecimal totalAmount = BigDecimal.ZERO;
            for (Order order : orders) {
                List<OrderItem> items = itemsByOrder.getOrDefault(order.getId(), java.util.Collections.emptyList());
                BigDecimal realAmount = BigDecimal.ZERO;
                for (OrderItem item : items) {
                    if (item.getIsGift() != null && item.getIsGift() == 1) {
                        continue; // 排除赠品行
                    }
                    realAmount = realAmount.add(item.getPrice().multiply(new BigDecimal(item.getQuantity())));
                }
                totalAmount = totalAmount.add(realAmount);

                String bucket = bucketKey(order.getCreateTime(), granularity);
                amountByBucket.merge(bucket, realAmount, BigDecimal::add);
                ordersByBucket.merge(bucket, 1, Integer::sum);
            }

            List<Map<String, Object>> trend = new ArrayList<>();
            for (String bucket : amountByBucket.keySet()) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("date", bucket);
                entry.put("amount", amountByBucket.get(bucket));
                entry.put("orders", ordersByBucket.getOrDefault(bucket, 0));
                trend.add(entry);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("totalAmount", totalAmount);
            result.put("totalOrders", totalOrders);
            result.put("trend", trend);
            return result;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalAmount", BigDecimal.ZERO);
        result.put("totalOrders", 0);
        result.put("trend", java.util.Collections.emptyList());
        return result;
    }

    /**
     * 热销商品 TOP10（排除赠品行）。
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTopProducts(Long shopId) {
        log.info("[商家统计] 热销TOP10 shopId={}", shopId);

        var qw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order>()
                .eq(Order::getShopId, shopId)
                .eq(Order::getIsDeleted, 0);
        List<Order> orders = orderMapper.selectList(qw);

        if (orders.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        // 批量加载所有订单的商品明细（消除 N+1 查询）
        List<Long> orderIds = orders.stream().map(Order::getId).collect(java.util.stream.Collectors.toList());
        List<OrderItem> allItems = orderItemMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrderItem>()
                        .in(OrderItem::getOrderId, orderIds)
        );

        Map<Long, BigDecimal> salesByProduct = new LinkedHashMap<>();
        Map<Long, String> nameByProduct = new HashMap<>();
        Map<Long, Integer> qtyByProduct = new HashMap<>();

        for (OrderItem item : allItems) {
            if (item.getIsGift() != null && item.getIsGift() == 1) continue;
            Long pid = item.getProductId();
            BigDecimal line = item.getPrice().multiply(new BigDecimal(item.getQuantity()));
            salesByProduct.merge(pid, line, BigDecimal::add);
            qtyByProduct.merge(pid, item.getQuantity(), Integer::sum);
            nameByProduct.putIfAbsent(pid, item.getProductNameSnapshot());
        }

        return salesByProduct.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(10)
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("productId", e.getKey());
                    m.put("name", nameByProduct.getOrDefault(e.getKey(), ""));
                    m.put("sales", qtyByProduct.getOrDefault(e.getKey(), 0));
                    m.put("amount", e.getValue());
                    return m;
                })
                .toList();
    }

    private String bucketKey(LocalDateTime time, String granularity) {
        LocalDate d = time.toLocalDate();
        return switch (granularity) {
            case "week" -> d.toString();
            case "month" -> d.getYear() + "-" + String.format("%02d", d.getMonthValue());
            default -> d.toString();
        };
    }
}
