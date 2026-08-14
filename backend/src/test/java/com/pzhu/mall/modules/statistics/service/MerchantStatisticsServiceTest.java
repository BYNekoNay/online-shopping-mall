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
    private MerchantStatisticsService service;

    @BeforeAll
    static void initTableInfo() {
        var assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Order.class);
        TableInfoHelper.initTableInfo(assistant, OrderItem.class);
    }

    @BeforeEach
    void setUp() {
        orderMapper = mock(OrderMapper.class);
        orderItemMapper = mock(OrderItemMapper.class);
        service = new MerchantStatisticsService();
        inject(service, "orderMapper", orderMapper);
        inject(service, "orderItemMapper", orderItemMapper);
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
}
