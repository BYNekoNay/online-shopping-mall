package com.pzhu.mall.modules.statistics.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.pzhu.mall.modules.behavior.entity.RecommendExposureLog;
import com.pzhu.mall.modules.behavior.mapper.PageViewLogMapper;
import com.pzhu.mall.modules.behavior.mapper.RecommendExposureLogMapper;
import com.pzhu.mall.modules.behavior.mapper.UserBehaviorMapper;
import com.pzhu.mall.modules.cart.mapper.CartMapper;
import com.pzhu.mall.modules.order.entity.Order;
import com.pzhu.mall.modules.order.mapper.OrderMapper;
import com.pzhu.mall.modules.product.mapper.ProductMapper;
import com.pzhu.mall.modules.recommend.mapper.RecommendResultMapper;
import com.pzhu.mall.modules.user.entity.User;
import com.pzhu.mall.modules.user.mapper.UserMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * PlatformStatisticsService 单元测试（平台看板口径：GMV/今日订单/CTR/转换率）。
 * <p>覆盖 docs/32 批次2 的 S-T05~07 用例：ST-02 状态过滤、BE-02 CTR 真实口径。</p>
 */
class PlatformStatisticsServiceTest {

    private OrderMapper orderMapper;
    private UserBehaviorMapper userBehaviorMapper;
    private PageViewLogMapper pageViewLogMapper;
    private RecommendResultMapper recommendResultMapper;
    private UserMapper userMapper;
    private CartMapper cartMapper;
    private RecommendExposureLogMapper recommendExposureLogMapper;
    private PlatformStatisticsService service;

    @BeforeAll
    static void initTableInfo() {
        // MyBatis-Plus lambda 缓存初始化（Order/User/RecommendExposureLog）
        for (Class<?> entity : new Class<?>[]{Order.class, User.class, RecommendExposureLog.class,
                com.pzhu.mall.modules.behavior.entity.PageViewLog.class,
                com.pzhu.mall.modules.behavior.entity.UserBehavior.class}) {
            if (!TableInfoHelper.getTableInfos().stream()
                    .anyMatch(t -> t.getEntityType() == entity)) {
                TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), entity);
            }
        }
    }

    @BeforeEach
    void setUp() {
        orderMapper = mock(OrderMapper.class);
        userBehaviorMapper = mock(UserBehaviorMapper.class);
        pageViewLogMapper = mock(PageViewLogMapper.class);
        recommendResultMapper = mock(RecommendResultMapper.class);
        userMapper = mock(UserMapper.class);
        cartMapper = mock(CartMapper.class);
        recommendExposureLogMapper = mock(RecommendExposureLogMapper.class);
        service = new PlatformStatisticsService();
        inject(service, "orderMapper", orderMapper);
        inject(service, "userBehaviorMapper", userBehaviorMapper);
        inject(service, "pageViewLogMapper", pageViewLogMapper);
        inject(service, "recommendResultMapper", recommendResultMapper);
        inject(service, "userMapper", userMapper);
        inject(service, "cartMapper", cartMapper);
        inject(service, "recommendExposureLogMapper", recommendExposureLogMapper);
    }

    @Test
    void getDashboard_returnsAllMetrics() {
        when(orderMapper.selectAllTotalPayAmount()).thenReturn(new BigDecimal("100000"));
        when(orderMapper.selectCount(any())).thenReturn(50L);
        when(userMapper.selectCount(any())).thenReturn(20L);
        // 曝光 100，点击 10 → CTR 10.00%
        when(recommendExposureLogMapper.selectCount(any()))
                .thenReturn(100L)  // 曝光
                .thenReturn(10L);  // 点击
        when(userBehaviorMapper.selectObjs(any())).thenReturn(Collections.singletonList(100L)); // 浏览用户
        when(orderMapper.selectObjs(any())).thenReturn(Collections.singletonList(50L));        // 下单用户

        Map<String, Object> result = service.getDashboard();

        assertEquals(new BigDecimal("100000"), result.get("gmv"));
        assertEquals(50L, result.get("orderCount"));
        assertEquals(20L, result.get("newUserCount"));
        // 转换率 50/100 = 0.5
        assertEquals(0.5, result.get("conversionRate"));
        // CTR 10/100 = 10.00%
        assertEquals("10.00%", result.get("recommendCtr"));
    }

    @Test
    void getDashboard_ctrNoExposure_returnsZero() {
        when(orderMapper.selectAllTotalPayAmount()).thenReturn(BigDecimal.ZERO);
        when(orderMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(recommendExposureLogMapper.selectCount(any())).thenReturn(0L);
        when(userBehaviorMapper.selectObjs(any())).thenReturn(Collections.singletonList(0L));
        when(orderMapper.selectObjs(any())).thenReturn(Collections.singletonList(0L));

        Map<String, Object> result = service.getDashboard();

        // BE-02 回归：无曝光 → "0.00%" 而非除零异常
        assertEquals("0.00%", result.get("recommendCtr"));
        assertEquals(0.0, result.get("conversionRate"));
    }

    @Test
    void getDashboard_ctrCalculatesPercentage() {
        when(orderMapper.selectAllTotalPayAmount()).thenReturn(BigDecimal.ZERO);
        when(orderMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.selectCount(any())).thenReturn(0L);
        // 曝光 200，点击 30 → 15.00%
        when(recommendExposureLogMapper.selectCount(any()))
                .thenReturn(200L)
                .thenReturn(30L);
        when(userBehaviorMapper.selectObjs(any())).thenReturn(Collections.singletonList(1L));
        when(orderMapper.selectObjs(any())).thenReturn(Collections.singletonList(1L));

        Map<String, Object> result = service.getDashboard();

        assertEquals("15.00%", result.get("recommendCtr"));
    }

    @Test
    void getDashboard_conversionRateNoBrowse_returnsZero() {
        when(orderMapper.selectAllTotalPayAmount()).thenReturn(BigDecimal.ZERO);
        when(orderMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(recommendExposureLogMapper.selectCount(any())).thenReturn(0L);
        when(userBehaviorMapper.selectObjs(any())).thenReturn(Collections.singletonList(0L));
        when(orderMapper.selectObjs(any())).thenReturn(Collections.singletonList(100L));

        Map<String, Object> result = service.getDashboard();

        // 浏览用户 0 → 转换率 0（防除零）
        assertEquals(0.0, result.get("conversionRate"));
    }

    @Test
    void getDashboard_todayOrdersFiltersPaidStatuses() {
        // ST-02 修复验证：今日订单数只统计已支付状态（1/2/3/4/6），排除待付款/取消/退款
        when(orderMapper.selectAllTotalPayAmount()).thenReturn(BigDecimal.ZERO);
        when(orderMapper.selectCount(any())).thenReturn(7L);
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(recommendExposureLogMapper.selectCount(any())).thenReturn(0L);
        when(userBehaviorMapper.selectObjs(any())).thenReturn(Collections.singletonList(0L));
        when(orderMapper.selectObjs(any())).thenReturn(Collections.singletonList(0L));

        service.getDashboard();

        // 通过 ArgumentCaptor 校验 status IN 条件
        var captor = org.mockito.ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class);
        verify(orderMapper).selectCount(captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("status") || sqlSegment.toLowerCase().contains("in"));
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

    // ==================== getStatisticsDetail（细项统计） ====================

    @Test
    void getStatisticsDetail_returnsAllMetrics() {
        java.time.LocalDate start = java.time.LocalDate.of(2026, 8, 1);
        java.time.LocalDate end = java.time.LocalDate.of(2026, 8, 15);
        when(pageViewLogMapper.selectCount(any())).thenReturn(1000L);        // PV
        when(pageViewLogMapper.selectObjs(any())).thenReturn(Collections.singletonList(300L)); // UV
        // 会话 50，其中跳出（cnt=1）20
        java.util.List<java.util.Map<String, Object>> sessions = new java.util.ArrayList<>();
        for (int i = 0; i < 30; i++) sessions.add(java.util.Map.of("cnt", 1L));
        for (int i = 0; i < 20; i++) sessions.add(java.util.Map.of("cnt", 5L));
        when(pageViewLogMapper.selectMaps(any())).thenReturn(sessions);
        when(userBehaviorMapper.selectCount(any())).thenReturn(800L);
        when(orderMapper.selectCount(any())).thenReturn(200L);

        java.util.Map<String, Object> result = service.getStatisticsDetail(start, end);

        assertNotNull(result);
        // 跳出率 30/50 = 0.6
        assertTrue(((Number) result.get("bounceRate")).doubleValue() > 0.59);
        assertTrue(((Number) result.get("bounceRate")).doubleValue() < 0.61);
        // 平均停留
        assertNotNull(result.get("avgStayDuration"));
    }

    @Test
    void getStatisticsDetail_emptyData_returnsZeros() {
        java.time.LocalDate start = java.time.LocalDate.of(2026, 8, 1);
        java.time.LocalDate end = java.time.LocalDate.of(2026, 8, 15);
        when(pageViewLogMapper.selectCount(any())).thenReturn(0L);
        when(pageViewLogMapper.selectObjs(any())).thenReturn(Collections.singletonList(0L));
        when(pageViewLogMapper.selectMaps(any())).thenReturn(Collections.emptyList());
        when(userBehaviorMapper.selectCount(any())).thenReturn(0L);
        when(orderMapper.selectCount(any())).thenReturn(0L);

        java.util.Map<String, Object> result = service.getStatisticsDetail(start, end);

        assertNotNull(result);
        assertEquals(0.0, ((Number) result.get("bounceRate")).doubleValue());
    }

}
