package com.pzhu.mall.modules.marketing.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.modules.marketing.entity.Coupon;
import com.pzhu.mall.modules.marketing.entity.UserCoupon;
import com.pzhu.mall.modules.marketing.mapper.CouponMapper;
import com.pzhu.mall.modules.marketing.mapper.UserCouponMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * 6.4 CouponService 单元测试。
 * <p>覆盖满减折扣计算、领取防超发（乐观更新）、核销归属校验与并发重复核销（H-08）、
 * 订单取消释放。</p>
 */
class CouponServiceTest {

    private CouponMapper couponMapper;
    private UserCouponMapper userCouponMapper;
    private CouponService service;

    @BeforeAll
    static void initTableInfo() {
        var assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Coupon.class);
        TableInfoHelper.initTableInfo(assistant, UserCoupon.class);
    }

    @BeforeEach
    void setUp() {
        couponMapper = mock(CouponMapper.class);
        userCouponMapper = mock(UserCouponMapper.class);
        service = new CouponService();
        inject(service, "couponMapper", couponMapper);
        inject(service, "userCouponMapper", userCouponMapper);
    }

    // ==================== calculateDiscount ====================

    @Test
    void calculateDiscount_meetsThreshold_returnsDiscount() {
        when(couponMapper.selectById(1L)).thenReturn(coupon("{\"threshold\":100,\"discount\":20}"));
        assertEquals(new BigDecimal(20), service.calculateDiscount(1L, new BigDecimal("150")));
        // 恰好等于门槛也可用
        assertEquals(new BigDecimal(20), service.calculateDiscount(1L, new BigDecimal("100")));
    }

    @Test
    void calculateDiscount_belowThreshold_returnsZero() {
        when(couponMapper.selectById(1L)).thenReturn(coupon("{\"threshold\":100,\"discount\":20}"));
        assertEquals(BigDecimal.ZERO, service.calculateDiscount(1L, new BigDecimal("99.99")));
    }

    @Test
    void calculateDiscount_expired_returnsZero() {
        Coupon c = coupon("{\"threshold\":100,\"discount\":20}");
        c.setValidTo(LocalDateTime.now().minusDays(1));
        when(couponMapper.selectById(1L)).thenReturn(c);
        assertEquals(BigDecimal.ZERO, service.calculateDiscount(1L, new BigDecimal("150")));
    }

    @Test
    void calculateDiscount_invalidJson_returnsZero() {
        when(couponMapper.selectById(1L)).thenReturn(coupon("not-a-json"));
        assertEquals(BigDecimal.ZERO, service.calculateDiscount(1L, new BigDecimal("150")));
    }

    @Test
    void calculateDiscount_stringRule_noDbAccess() {
        assertEquals(new BigDecimal(30),
                service.calculateDiscount("{\"threshold\":200,\"discount\":30}", new BigDecimal("250")));
        assertEquals(BigDecimal.ZERO,
                service.calculateDiscount("{\"threshold\":200,\"discount\":30}", new BigDecimal("100")));
        assertEquals(BigDecimal.ZERO, service.calculateDiscount((String) null, new BigDecimal("100")));
    }

    @Test
    void calculateDiscount_rateRule_returnsPercentageOff() {
        // M-09 修复验证：折扣率规则 rate=0.8（八折），满 100 可用，150 × (1-0.8) = 30.00
        when(couponMapper.selectById(1L)).thenReturn(coupon("{\"threshold\":100,\"rate\":0.8}"));
        assertEquals(new BigDecimal("30.00"), service.calculateDiscount(1L, new BigDecimal("150")));
        // 低于门槛不可用
        assertEquals(BigDecimal.ZERO, service.calculateDiscount(1L, new BigDecimal("99")));
    }

    @Test
    void calculateDiscount_rateRuleInvalid_returnsZero() {
        // M-09 修复验证：rate 超出 (0,1) 视为非法规则
        when(couponMapper.selectById(1L)).thenReturn(coupon("{\"threshold\":100,\"rate\":1.5}"));
        assertEquals(BigDecimal.ZERO, service.calculateDiscount(1L, new BigDecimal("150")));
    }

    // ==================== markUsed (H-08) ====================

    @Test
    void markUsed_success() {
        UserCoupon uc = userCoupon(7L, 100L);
        when(userCouponMapper.selectById(7L)).thenReturn(uc);
        when(userCouponMapper.update(any(UserCoupon.class), any())).thenReturn(1);

        assertDoesNotThrow(() -> service.markUsed(7L, 55L, 100L));

        ArgumentCaptor<UserCoupon> captor = ArgumentCaptor.forClass(UserCoupon.class);
        verify(userCouponMapper).update(captor.capture(), any());
        assertEquals(1, captor.getValue().getStatus());
        assertEquals(55L, captor.getValue().getRelatedOrderId());
        assertNotNull(captor.getValue().getUseTime());
    }

    @Test
    void markUsed_notOwner_throws() {
        // H-08：他人优惠券不得核销
        when(userCouponMapper.selectById(7L)).thenReturn(userCoupon(7L, 999L));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.markUsed(7L, 55L, 100L));
        assertEquals(ErrorCode.COUPON_UNAVAILABLE.getCode(), ex.getCode());
        verify(userCouponMapper, never()).update(any(UserCoupon.class), any());
    }

    @Test
    void markUsed_notFound_throws() {
        when(userCouponMapper.selectById(7L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.markUsed(7L, 55L, 100L));
        assertEquals(ErrorCode.COUPON_UNAVAILABLE.getCode(), ex.getCode());
    }

    @Test
    void markUsed_concurrentDoubleUse_throws() {
        // H-08 修复验证：WHERE status=0 影响行数为 0 → 已被并发核销 → 抛异常回滚订单事务
        when(userCouponMapper.selectById(7L)).thenReturn(userCoupon(7L, 100L));
        when(userCouponMapper.update(any(UserCoupon.class), any())).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.markUsed(7L, 55L, 100L));
        assertEquals(ErrorCode.COUPON_UNAVAILABLE.getCode(), ex.getCode());
    }

    // ==================== receive ====================

    @Test
    void receive_success_returnsUserCouponId() {
        Coupon c = coupon("{\"threshold\":100,\"discount\":20}");
        c.setStock(10);
        c.setReceivedCount(3);
        when(couponMapper.selectById(1L)).thenReturn(c);
        when(couponMapper.update(isNull(), any())).thenReturn(1);
        when(userCouponMapper.insert(any(UserCoupon.class))).thenAnswer(inv -> {
            inv.getArgument(0, UserCoupon.class).setId(123L);
            return 1;
        });

        Long id = service.receive(100L, 1L);

        assertEquals(123L, id);
        ArgumentCaptor<UserCoupon> captor = ArgumentCaptor.forClass(UserCoupon.class);
        verify(userCouponMapper).insert(captor.capture());
        assertEquals(100L, captor.getValue().getUserId());
        assertEquals(0, captor.getValue().getStatus());
    }

    @Test
    void receive_soldOut_throws() {
        Coupon c = coupon("{}");
        c.setStock(10);
        c.setReceivedCount(10);
        when(couponMapper.selectById(1L)).thenReturn(c);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.receive(100L, 1L));
        assertEquals(ErrorCode.COUPON_SOLD_OUT.getCode(), ex.getCode());
    }

    @Test
    void receive_expired_throws() {
        Coupon c = coupon("{}");
        c.setStock(10);
        c.setReceivedCount(0);
        c.setValidTo(LocalDateTime.now().minusDays(1));
        when(couponMapper.selectById(1L)).thenReturn(c);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.receive(100L, 1L));
        assertEquals(ErrorCode.COUPON_EXPIRED.getCode(), ex.getCode());
    }

    @Test
    void receive_notStarted_throws() {
        // M-08 修复验证：validFrom 在未来 → 领取尚未开始
        Coupon c = coupon("{}");
        c.setStock(10);
        c.setReceivedCount(0);
        c.setValidFrom(LocalDateTime.now().plusDays(1));
        when(couponMapper.selectById(1L)).thenReturn(c);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.receive(100L, 1L));
        assertEquals(ErrorCode.COUPON_NOT_STARTED.getCode(), ex.getCode());
    }

    @Test
    void receive_optimisticLockLost_throws() {
        // 并发领取时乐观更新（received_count CAS）影响 0 行 → 视为抢光
        Coupon c = coupon("{}");
        c.setStock(10);
        c.setReceivedCount(9);
        when(couponMapper.selectById(1L)).thenReturn(c);
        when(couponMapper.update(isNull(), any())).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.receive(100L, 1L));
        assertEquals(ErrorCode.COUPON_SOLD_OUT.getCode(), ex.getCode());
        verify(userCouponMapper, never()).insert(any(UserCoupon.class));
    }

    // ==================== releaseByOrderId ====================

    @Test
    void releaseByOrderId_resetsUsedCoupon() {
        when(userCouponMapper.update(isNull(), any())).thenReturn(1);
        assertDoesNotThrow(() -> service.releaseByOrderId(55L));
        verify(userCouponMapper).update(isNull(), any());
    }

    // ==================== helpers ====================

    private static Coupon coupon(String rule) {
        Coupon c = new Coupon();
        c.setId(1L);
        c.setDiscountRule(rule);
        c.setValidTo(LocalDateTime.now().plusDays(7));
        c.setIsDeleted(0);
        c.setStock(100);
        c.setReceivedCount(0);
        return c;
    }

    private static UserCoupon userCoupon(Long id, Long userId) {
        UserCoupon uc = new UserCoupon();
        uc.setId(id);
        uc.setUserId(userId);
        uc.setCouponId(1L);
        uc.setStatus(0);
        return uc;
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
