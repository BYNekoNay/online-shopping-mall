package com.pzhu.mall.modules.marketing.controller;

import com.pzhu.mall.modules.marketing.entity.Coupon;
import com.pzhu.mall.modules.marketing.service.CouponService;
import com.pzhu.mall.modules.marketing.vo.UserCouponVO;
import com.pzhu.mall.security.LoginUserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * CouponController 单元测试（领取/列表）。
 */
class CouponControllerTest {

    private CouponService couponService;
    private com.pzhu.mall.modules.marketing.mapper.CouponMapper couponMapper;
    private CouponController controller;

    @BeforeEach
    void setUp() {
        couponService = mock(CouponService.class);
        couponMapper = mock(com.pzhu.mall.modules.marketing.mapper.CouponMapper.class);
        controller = new CouponController();
        inject(controller, "couponService", couponService);
        inject(controller, "couponMapper", couponMapper);
    }

    @AfterEach
    void tearDown() {
        LoginUserContext.clear();
    }

    @Test
    void available_returnsCoupons() {
        LoginUserContext.set(100L, 1);
        List<Coupon> coupons = Collections.singletonList(new Coupon());
        when(couponService.listAvailable(100L)).thenReturn(coupons);

        var result = controller.available();

        assertEquals(1, result.getData().size());
        verify(couponService).listAvailable(100L);
    }

    @Test
    void receive_returnsUserCouponId() {
        LoginUserContext.set(100L, 1);
        when(couponService.receive(100L, 5L)).thenReturn(99L);

        var result = controller.receive(5L);

        assertEquals(99L, result.getData().get("userCouponId"));
        verify(couponService).receive(100L, 5L);
    }

    @Test
    void userCoupons_returnsList() {
        LoginUserContext.set(100L, 1);
        when(couponService.listUserCoupons(100L, 0))
                .thenReturn(java.util.Collections.emptyList());

        var result = controller.userCoupons(0);

        assertNotNull(result.getData());
        verify(couponService).listUserCoupons(100L, 0);
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
