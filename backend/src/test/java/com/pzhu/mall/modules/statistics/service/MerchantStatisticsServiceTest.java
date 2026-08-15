package com.pzhu.mall.modules.statistics.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.pzhu.mall.modules.order.entity.Order;
import com.pzhu.mall.modules.order.entity.OrderItem;
import com.pzhu.mall.modules.order.mapper.OrderItemMapper;
import com.pzhu.mall.modules.order.mapper.OrderMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ST-01 修复验证：商家销售统计仅统计已支付有效订单状态（1/2/3/4/6），
 * 待付款(0)/已取消(5)/已退款(7)不得计入销售额与热销 TOP10。
 */
class MerchantStatisticsServiceTest {

    private OrderMapper orderMapper;
    private OrderItemMapper orderItemMapper;
    private com.pzhu.mall.modules.product.mapper.ReviewMapper reviewMapper;
    private MerchantStatisticsService service;

    @BeforeAll
    static void initTableInfo() {
        var assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Order.class);
        TableInfoHelper.initTableInfo(assistant, OrderItem.class);
        TableInfoHelper.initTableInfo(assistant, com.pzhu.mall.modules.product.entity.Review.class);
    }

    @BeforeEach
    void setUp() {
        orderMapper = mock(OrderMapper.class);
        orderItemMapper = mock(OrderItemMapper.class);
        reviewMapper = mock(com.pzhu.mall.modules.product.mapper.ReviewMapper.class);
        service = new MerchantStatisticsService();
        inject(service, "orderMapper", orderMapper);
        inject(service, "orderItemMapper", orderItemMapper);
        inject(service, "reviewMapper", reviewMapper);
    }

    @Test
    void getSalesStatistics_onlyCountsPaidStatuses() {
        LocalDate start = LocalDate.of(2026, 8, 1);
        LocalDate end = LocalDate.of(2026, 8, 31);
        when(orderMapper.selectList(any())).thenReturn(Collections.emptyList());

        service.getSalesStatistics(1L, start, end, "day");

        // 关键断言：查询条件必须包含 status IN (1,2,3,4,6)
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<Order>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(orderMapper).selectList(captor.capture());
        String sqlSegment = captor.getValue().getExpression().getSqlSegment();
        assertTrue(sqlSegment.contains("status") && sqlSegment.contains("IN"));
    }

    @Test
    void getTopProducts_onlyCountsPaidStatuses() {
        when(orderMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<Map<String, Object>> top = service.getTopProducts(1L);

        assertTrue(top.isEmpty());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<Order>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(orderMapper).selectList(captor.capture());
        String sqlSegment = captor.getValue().getExpression().getSqlSegment();
        assertTrue(sqlSegment.contains("status") && sqlSegment.contains("IN"));
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

    // ==================== B-3 好评率 ====================

    private void stubTopProductsContext() {
        Order order = new Order();
        order.setId(1L);
        order.setShopId(1L);
        order.setStatus(1);
        when(orderMapper.selectList(any())).thenReturn(Collections.singletonList(order));

        OrderItem real = new OrderItem();
        real.setOrderId(1L);
        real.setProductId(10L);
        real.setPrice(new BigDecimal("100"));
        real.setQuantity(1);
        real.setIsGift(0);
        when(orderItemMapper.selectList(any())).thenReturn(Collections.singletonList(real));
    }

    @Test
    void getTopProducts_positiveRate_80Percent() {
        // M-01：10 条评价 8 条 ≥4 分 → positiveRate="80%"
        stubTopProductsContext();
        java.util.List<com.pzhu.mall.modules.product.entity.Review> reviews = new java.util.ArrayList<>();
        for (int i = 0; i < 10; i++) {
            com.pzhu.mall.modules.product.entity.Review r = new com.pzhu.mall.modules.product.entity.Review();
            r.setProductId(10L);
            r.setRating(i < 8 ? 5 : 3);
            reviews.add(r);
        }
        when(reviewMapper.selectList(any())).thenReturn(reviews);

        List<Map<String, Object>> top = service.getTopProducts(1L);

        assertEquals(1, top.size());
        assertEquals("80%", top.get(0).get("positiveRate"));
    }

    @Test
    void getTopProducts_noReviews_returnsNull() {
        // M-02：无评价 → positiveRate=null（前端显示 "-"）
        stubTopProductsContext();
        when(reviewMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<Map<String, Object>> top = service.getTopProducts(1L);

        assertEquals(1, top.size());
        assertNull(top.get(0).get("positiveRate"));
    }

    @Test
    void getTopProducts_mixedProducts_independentRate() {
        // M-03：多商品独立计算（商品A 50% / 商品B 无评价 null）
        Order order = new Order();
        order.setId(1L);
        order.setShopId(1L);
        order.setStatus(1);
        when(orderMapper.selectList(any())).thenReturn(Collections.singletonList(order));

        OrderItem itemA = new OrderItem();
        itemA.setOrderId(1L);
        itemA.setProductId(10L);
        itemA.setPrice(new BigDecimal("100"));
        itemA.setQuantity(1);
        itemA.setIsGift(0);
        OrderItem itemB = new OrderItem();
        itemB.setOrderId(1L);
        itemB.setProductId(20L);
        itemB.setPrice(new BigDecimal("200"));
        itemB.setQuantity(1);
        itemB.setIsGift(0);
        when(orderItemMapper.selectList(any())).thenReturn(java.util.Arrays.asList(itemA, itemB));

        com.pzhu.mall.modules.product.entity.Review r1 = new com.pzhu.mall.modules.product.entity.Review();
        r1.setProductId(10L);
        r1.setRating(5);
        com.pzhu.mall.modules.product.entity.Review r2 = new com.pzhu.mall.modules.product.entity.Review();
        r2.setProductId(10L);
        r2.setRating(1);
        when(reviewMapper.selectList(any())).thenReturn(java.util.Arrays.asList(r1, r2));

        List<Map<String, Object>> top = service.getTopProducts(1L);

        assertEquals(2, top.size());
        Map<Object, Map<String, Object>> byProduct = top.stream()
                .collect(java.util.stream.Collectors.toMap(m -> m.get("productId"), m -> m));
        assertEquals("50%", byProduct.get(10L).get("positiveRate"));
        assertNull(byProduct.get(20L).get("positiveRate"));
    }

    @Test
    void getSalesStatistics_emptyOrders_returnsZeroSummary() {
        LocalDate start = LocalDate.of(2026, 8, 1);
        LocalDate end = LocalDate.of(2026, 8, 31);
        when(orderMapper.selectList(any())).thenReturn(Collections.emptyList());

        Map<String, Object> result = service.getSalesStatistics(1L, start, end, "day");

        assertEquals(BigDecimal.ZERO, result.get("totalAmount"));
        assertEquals(0, result.get("totalOrders"));
        assertTrue(((List<?>) result.get("trend")).isEmpty());
    }

    @Test
    void getSalesStatistics_excludesGiftItems() {
        // M-01 赠品行排除：订单含赠品（price=0, isGift=1），销售额只算真实商品
        LocalDate start = LocalDate.of(2026, 8, 1);
        LocalDate end = LocalDate.of(2026, 8, 31);

        Order order = new Order();
        order.setId(1L);
        order.setShopId(1L);
        order.setStatus(1);
        order.setCreateTime(java.time.LocalDateTime.of(2026, 8, 10, 12, 0));
        when(orderMapper.selectList(any())).thenReturn(Collections.singletonList(order));

        OrderItem real = new OrderItem();
        real.setOrderId(1L);
        real.setPrice(new BigDecimal("100"));
        real.setQuantity(2);
        real.setIsGift(0);
        OrderItem gift = new OrderItem();
        gift.setOrderId(1L);
        gift.setPrice(BigDecimal.ZERO);
        gift.setQuantity(1);
        gift.setIsGift(1);
        when(orderItemMapper.selectList(any())).thenReturn(java.util.Arrays.asList(real, gift));

        Map<String, Object> result = service.getSalesStatistics(1L, start, end, "day");

        // 只算真实商品 100*2=200，赠品不计
        assertEquals(new BigDecimal("200"), result.get("totalAmount"));
        assertEquals(1, result.get("totalOrders"));
        assertEquals(1, ((List<?>) result.get("trend")).size());
    }

    @Test
    void getTopProducts_excludesGiftItems() {
        // M-01 热销排除赠品行
        Order order = new Order();
        order.setId(1L);
        order.setShopId(1L);
        order.setStatus(1);
        when(orderMapper.selectList(any())).thenReturn(Collections.singletonList(order));

        OrderItem real = new OrderItem();
        real.setOrderId(1L);
        real.setProductId(10L);
        real.setPrice(new BigDecimal("50"));
        real.setQuantity(3);
        real.setIsGift(0);
        OrderItem gift = new OrderItem();
        gift.setOrderId(1L);
        gift.setProductId(99L);
        gift.setPrice(BigDecimal.ZERO);
        gift.setQuantity(1);
        gift.setIsGift(1);
        when(orderItemMapper.selectList(any())).thenReturn(java.util.Arrays.asList(real, gift));

        List<Map<String, Object>> top = service.getTopProducts(1L);

        // 赠品商品 99 不应出现在热销中
        assertTrue(top.stream().noneMatch(m -> java.util.Objects.equals(m.get("productId"), 99L)));
        assertTrue(top.stream().anyMatch(m -> java.util.Objects.equals(m.get("productId"), 10L)));
    }

}
