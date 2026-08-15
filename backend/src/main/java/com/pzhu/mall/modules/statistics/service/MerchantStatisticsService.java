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

    // B-3 审核修正：好评率需查评价表，补注入（此前未注入）
    @Resource
    private com.pzhu.mall.modules.product.mapper.ReviewMapper reviewMapper;

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
        // ST-01 修复：仅统计已支付有效订单（1待发货/2已发货/3已收货/4已完成/6退款中），
        // 待付款(0)/已取消(5)/已退款(7)不产生有效销售，与平台 GMV 口径一致（此前全部计入导致虚高）
        var qw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order>()
                .eq(Order::getShopId, shopId)
                .eq(Order::getIsDeleted, 0)
                .in(Order::getStatus, 1, 2, 3, 4, 6)
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
            // WMI_WRONG_MAP_ITERATOR 修复：直接遍历 entrySet，避免 keySet 循环内重复 get
            for (Map.Entry<String, BigDecimal> bucketEntry : amountByBucket.entrySet()) {
                String bucket = bucketEntry.getKey();
                Map<String, Object> entry = new HashMap<>();
                entry.put("date", bucket);
                entry.put("amount", bucketEntry.getValue());
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

        // ST-01 修复：热销 TOP10 同口径过滤订单状态，避免待付款/取消/退款订单虚高
        var qw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order>()
                .eq(Order::getShopId, shopId)
                .eq(Order::getIsDeleted, 0)
                .in(Order::getStatus, 1, 2, 3, 4, 6);
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

        // B-3：批量加载 TOP10 商品的评价，计算好评率（评分≥4 占比；无评价 → null）
        List<Long> topProductIds = salesByProduct.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toList());
        Map<Long, double[]> reviewStat = reviewMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
                                com.pzhu.mall.modules.product.entity.Review>()
                                .in(com.pzhu.mall.modules.product.entity.Review::getProductId, topProductIds))
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        com.pzhu.mall.modules.product.entity.Review::getProductId,
                        java.util.stream.Collectors.collectingAndThen(
                                java.util.stream.Collectors.toList(),
                                list -> {
                                    long total = list.size();
                                    long good = list.stream()
                                            .filter(r -> r.getRating() != null && r.getRating() >= 4)
                                            .count();
                                    return new double[]{total, good};
                                })));

        return salesByProduct.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(10)
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("productId", e.getKey());
                    m.put("name", nameByProduct.getOrDefault(e.getKey(), ""));
                    m.put("sales", qtyByProduct.getOrDefault(e.getKey(), 0));
                    m.put("amount", e.getValue());
                    // B-3：好评率（无评价返回 null，前端显示 "-"）
                    double[] stat = reviewStat.get(e.getKey());
                    m.put("positiveRate", stat != null && stat[0] > 0
                            ? Math.round(stat[1] * 100.0 / stat[0]) + "%"
                            : null);
                    return m;
                })
                .toList();
    }

    private String bucketKey(LocalDateTime time, String granularity) {
        LocalDate d = time.toLocalDate();
        return switch (granularity) {
            // M-12 修复：周粒度取所在 ISO 周的周一作为桶键（原实现与 day 相同，按周聚合失效）
            case "week" -> d.with(java.time.DayOfWeek.MONDAY).toString();
            case "month" -> d.getYear() + "-" + String.format("%02d", d.getMonthValue());
            default -> d.toString();
        };
    }
}
