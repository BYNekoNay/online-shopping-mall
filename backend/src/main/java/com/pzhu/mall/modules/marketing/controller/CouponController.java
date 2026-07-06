package com.pzhu.mall.modules.marketing.controller;

import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.modules.marketing.entity.Coupon;
import com.pzhu.mall.modules.marketing.service.CouponService;
import com.pzhu.mall.modules.marketing.service.PromotionService;
import com.pzhu.mall.modules.marketing.entity.Promotion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 优惠券控制器（消费者+管理员共用）。
 */
@Tag(name = "优惠券")
@RestController
@RequestMapping("/api")
public class CouponController {

    @Resource
    private CouponService couponService;

    @Resource
    private PromotionService promotionService;

    @Operation(summary = "可用优惠券列表（消费者）")
    @GetMapping("/user/coupons")
    public Result<List<Coupon>> userCoupons() {
        Long userId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();
        return Result.success(couponService.listAvailable(userId));
    }

    @Operation(summary = "领取优惠券")
    @PostMapping("/coupons/{id}/receive")
    public Result<Void> receive(@PathVariable Long id) {
        Long userId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();
        couponService.receive(userId, id);
        return Result.success();
    }

    @Operation(summary = "进行中促销活动列表")
    @GetMapping("/promotions/active")
    public Result<List<Promotion>> activePromotions() {
        return Result.success(promotionService.listActive());
    }

    // ---- 管理员接口 ----

    @Operation(summary = "优惠券管理列表")
    @GetMapping("/admin/coupons")
    public Result<List<Coupon>> adminList() {
        return Result.success(couponService.listAll());
    }

    @Operation(summary = "创建优惠券")
    @PostMapping("/admin/coupons")
    public Result<Void> create(@RequestBody Coupon coupon) {
        couponService.create(coupon);
        return Result.success();
    }

    @Operation(summary = "创建促销活动")
    @PostMapping("/admin/promotions")
    public Result<Void> createPromotion(@RequestBody Promotion promotion) {
        promotionService.create(promotion);
        return Result.success();
    }
}
