package com.pzhu.mall.modules.order.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.modules.marketing.service.PointsService;
import com.pzhu.mall.modules.order.dto.RefundApplyDTO;
import com.pzhu.mall.modules.order.entity.Order;
import com.pzhu.mall.modules.order.entity.OrderItem;
import com.pzhu.mall.modules.order.entity.Refund;
import com.pzhu.mall.modules.order.mapper.OrderItemMapper;
import com.pzhu.mall.modules.order.mapper.OrderMapper;
import com.pzhu.mall.modules.order.mapper.RefundMapper;
import com.pzhu.mall.modules.order.vo.RefundVO;
import com.pzhu.mall.security.LoginUserContext;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * 6.5 RefundService 单元测试。
 * <p>覆盖退款申请（H-01 归属校验防 IDOR、M-04 订单行归属、赠品排除、幂等）、
 * 商家审核（店铺归属、原子状态流转、积分扣回）。</p>
 */
class RefundServiceTest {

    private RefundMapper refundMapper;
    private OrderMapper orderMapper;
    private OrderItemMapper orderItemMapper;
    private PointsService pointsService;
    private StringRedisTemplate stringRedisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RefundService service;

    @BeforeAll
    static void initTableInfo() {
        var assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Refund.class);
        TableInfoHelper.initTableInfo(assistant, Order.class);
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        refundMapper = mock(RefundMapper.class);
        orderMapper = mock(OrderMapper.class);
        orderItemMapper = mock(OrderItemMapper.class);
        pointsService = mock(PointsService.class);
        stringRedisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        service = new RefundService();
        inject(service, "refundMapper", refundMapper);
        inject(service, "orderMapper", orderMapper);
        inject(service, "orderItemMapper", orderItemMapper);
        inject(service, "pointsService", pointsService);
        inject(service, "stringRedisTemplate", stringRedisTemplate);

        LoginUserContext.set(100L, 1);
    }

    @AfterEach
    void tearDown() {
        LoginUserContext.clear();
    }

    // ==================== apply ====================

    @Test
    void apply_success_createsPendingRefund() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);
        when(orderMapper.selectById(5L)).thenReturn(order(5L, 100L, 8L, new BigDecimal("200.00")));

        RefundApplyDTO dto = applyDto(5L, null, new BigDecimal("100.00"));
        service.apply(dto);

        ArgumentCaptor<Refund> captor = ArgumentCaptor.forClass(Refund.class);
        verify(refundMapper).insert(captor.capture());
        Refund saved = captor.getValue();
        assertEquals(5L, saved.getOrderId());
        assertEquals(0, saved.getStatus()); // 待审核
        assertEquals(new BigDecimal("100.00"), saved.getAmount());
    }

    @Test
    void apply_duplicateRequest_throws() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.apply(applyDto(5L, null, new BigDecimal("100.00"))));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
        verify(refundMapper, never()).insert(any(Refund.class));
    }

    @Test
    void apply_notOwner_throws() {
        // H-01 修复验证：他人订单不可退款（IDOR 防护）
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);
        when(orderMapper.selectById(5L)).thenReturn(order(5L, 999L, 8L, new BigDecimal("200.00")));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.apply(applyDto(5L, null, new BigDecimal("100.00"))));
        assertEquals(ErrorCode.NOT_FOUND.getCode(), ex.getCode());
        verify(refundMapper, never()).insert(any(Refund.class));
    }

    @Test
    void apply_orderNotFound_throws() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);
        when(orderMapper.selectById(5L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.apply(applyDto(5L, null, new BigDecimal("100.00"))));
        assertEquals(ErrorCode.NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void apply_amountExceedsPayAmount_throws() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);
        when(orderMapper.selectById(5L)).thenReturn(order(5L, 100L, 8L, new BigDecimal("200.00")));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.apply(applyDto(5L, null, new BigDecimal("200.01"))));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void apply_amountNotPositive_throws() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);
        when(orderMapper.selectById(5L)).thenReturn(order(5L, 100L, 8L, new BigDecimal("200.00")));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.apply(applyDto(5L, null, BigDecimal.ZERO)));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void apply_giftItem_throws() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);
        when(orderMapper.selectById(5L)).thenReturn(order(5L, 100L, 8L, new BigDecimal("200.00")));
        OrderItem gift = new OrderItem();
        gift.setId(30L);
        gift.setOrderId(5L);
        gift.setIsGift(1);
        when(orderItemMapper.selectById(30L)).thenReturn(gift);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.apply(applyDto(5L, 30L, new BigDecimal("10.00"))));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void apply_itemFromOtherOrder_throws() {
        // M-04 修复验证：订单行必须属于当前订单，防止引用他人订单的行
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);
        when(orderMapper.selectById(5L)).thenReturn(order(5L, 100L, 8L, new BigDecimal("200.00")));
        OrderItem foreign = new OrderItem();
        foreign.setId(30L);
        foreign.setOrderId(6L); // 属于另一订单
        when(orderItemMapper.selectById(30L)).thenReturn(foreign);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.apply(applyDto(5L, 30L, new BigDecimal("10.00"))));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void apply_orderUnpaid_throwsStatusInvalid() {
        // H-7 修复验证：待付款(0)订单没有支付事实，不允许申请退款
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);
        Order unpaid = order(5L, 100L, 8L, new BigDecimal("200.00"));
        unpaid.setStatus(0);
        when(orderMapper.selectById(5L)).thenReturn(unpaid);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.apply(applyDto(5L, null, new BigDecimal("100.00"))));
        assertEquals(ErrorCode.ORDER_STATUS_INVALID.getCode(), ex.getCode());
        verify(refundMapper, never()).insert(any(Refund.class));
    }

    @Test
    void apply_orderCancelled_throwsStatusInvalid() {
        // H-7 修复验证：已取消(5)订单退款会造成 5→7 非法状态迁移
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);
        Order cancelled = order(5L, 100L, 8L, new BigDecimal("200.00"));
        cancelled.setStatus(5);
        when(orderMapper.selectById(5L)).thenReturn(cancelled);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.apply(applyDto(5L, null, new BigDecimal("100.00"))));
        assertEquals(ErrorCode.ORDER_STATUS_INVALID.getCode(), ex.getCode());
        verify(refundMapper, never()).insert(any(Refund.class));
    }

    @Test
    void apply_existingPendingRefund_throws() {
        // H-8 修复验证：同订单已有待审核退款记录时拒绝再次申请，防止重复退款/重复扣回积分
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);
        when(orderMapper.selectById(5L)).thenReturn(order(5L, 100L, 8L, new BigDecimal("200.00")));
        when(refundMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.apply(applyDto(5L, null, new BigDecimal("100.00"))));
        assertEquals(ErrorCode.ORDER_STATUS_INVALID.getCode(), ex.getCode());
        verify(refundMapper, never()).insert(any(Refund.class));
    }

    @Test
    void apply_onlyRejectedRefundExists_allowsReapply() {
        // H-8 边界：已驳回(status=2)的退款记录不阻塞用户重新申请
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);
        when(orderMapper.selectById(5L)).thenReturn(order(5L, 100L, 8L, new BigDecimal("200.00")));
        when(refundMapper.selectCount(any())).thenReturn(0L);

        service.apply(applyDto(5L, null, new BigDecimal("100.00")));

        verify(refundMapper).insert(any(Refund.class));
    }

    // ==================== audit ====================

    @Test
    void audit_approve_updatesOrderAndClawsBackPoints() {
        Refund refund = refund(1L, 5L, 0);
        when(refundMapper.selectById(1L)).thenReturn(refund);
        when(orderMapper.selectById(5L)).thenReturn(order(5L, 100L, 8L, new BigDecimal("200.00")));
        when(refundMapper.update(isNull(), any())).thenReturn(1);
        when(orderMapper.update(isNull(), any())).thenReturn(1);

        service.audit(1L, true, "同意退款", 8L);

        verify(orderMapper).update(isNull(), any());      // 订单 → 已退款(7)
        verify(pointsService).clawback(5L);               // 积分扣回
        verify(pointsService).refundDeduct(5L);           // H-5：返还下单抵扣积分
    }

    @Test
    void audit_reject_noOrderUpdateNoClawback() {
        Refund refund = refund(1L, 5L, 0);
        when(refundMapper.selectById(1L)).thenReturn(refund);
        when(orderMapper.selectById(5L)).thenReturn(order(5L, 100L, 8L, new BigDecimal("200.00")));
        when(refundMapper.update(isNull(), any())).thenReturn(1);

        service.audit(1L, false, "不符合退款条件", 8L);

        verify(orderMapper, never()).update(any(), any());
        verify(pointsService, never()).clawback(anyLong());
        verify(pointsService, never()).refundDeduct(anyLong());
    }

    @Test
    void audit_refundNotFound_throws() {
        when(refundMapper.selectById(1L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.audit(1L, true, "ok", 8L));
        assertEquals(ErrorCode.NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void audit_alreadyAudited_throws() {
        when(refundMapper.selectById(1L)).thenReturn(refund(1L, 5L, 1)); // 已通过
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.audit(1L, true, "ok", 8L));
        assertEquals(ErrorCode.ORDER_STATUS_INVALID.getCode(), ex.getCode());
    }

    @Test
    void audit_shopNotOwner_throws() {
        when(refundMapper.selectById(1L)).thenReturn(refund(1L, 5L, 0));
        when(orderMapper.selectById(5L)).thenReturn(order(5L, 100L, 9L, new BigDecimal("200.00"))); // 店铺 9 ≠ 8

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.audit(1L, true, "ok", 8L));
        assertEquals(ErrorCode.NOT_FOUND.getCode(), ex.getCode());
        verify(refundMapper, never()).update(any(), any());
    }

    @Test
    void audit_concurrentAuditLost_throws() {
        // M7 修复验证：WHERE status=0 影响 0 行 → 已被并发审核 → 不再扣积分
        when(refundMapper.selectById(1L)).thenReturn(refund(1L, 5L, 0));
        when(orderMapper.selectById(5L)).thenReturn(order(5L, 100L, 8L, new BigDecimal("200.00")));
        when(refundMapper.update(isNull(), any())).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.audit(1L, true, "ok", 8L));
        assertEquals(ErrorCode.ORDER_STATUS_INVALID.getCode(), ex.getCode());
        verify(pointsService, never()).clawback(anyLong());
    }

    // ==================== listByUser ====================

    @Test
    void listByUser_returnsOwnRefundsOnly() {
        Order o = new Order();
        o.setId(5L);
        when(orderMapper.selectList(any())).thenReturn(Collections.singletonList(o));
        when(refundMapper.selectList(any())).thenReturn(Collections.singletonList(refund(1L, 5L, 0)));

        List<RefundVO> vos = service.listByUser();
        assertEquals(1, vos.size());
        assertEquals(5L, vos.get(0).getOrderId());
    }

    @Test
    void listByUser_noOrders_returnsEmpty() {
        when(orderMapper.selectList(any())).thenReturn(Collections.emptyList());
        assertTrue(service.listByUser().isEmpty());
        verify(refundMapper, never()).selectList(any());
    }

    // ==================== helpers ====================

    private static RefundApplyDTO applyDto(Long orderId, Long orderItemId, BigDecimal amount) {
        RefundApplyDTO dto = new RefundApplyDTO();
        dto.setOrderId(orderId);
        dto.setOrderItemId(orderItemId);
        dto.setType(1);
        dto.setReason("不想要了");
        dto.setAmount(amount);
        return dto;
    }

    private static Order order(Long id, Long userId, Long shopId, BigDecimal payAmount) {
        Order o = new Order();
        o.setId(id);
        o.setUserId(userId);
        o.setShopId(shopId);
        o.setPayAmount(payAmount);
        o.setStatus(3);
        return o;
    }

    private static Refund refund(Long id, Long orderId, int status) {
        Refund r = new Refund();
        r.setId(id);
        r.setOrderId(orderId);
        r.setStatus(status);
        r.setAmount(new BigDecimal("100.00"));
        return r;
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
