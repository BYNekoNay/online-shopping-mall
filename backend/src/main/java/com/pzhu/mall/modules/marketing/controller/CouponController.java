package com.pzhu.mall.modules.marketing.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.modules.marketing.entity.Coupon;
import com.pzhu.mall.modules.marketing.entity.UserCoupon;
import com.pzhu.mall.modules.marketing.mapper.UserCouponMapper;
import com.pzhu.mall.modules.marketing.mapper.CouponMapper;
import com.pzhu.mall.modules.marketing.mapper.PromotionMapper;
import com.pzhu.mall.modules.marketing.service.CouponService;
import com.pzhu.mall.modules.marketing.service.PromotionService;
import com.pzhu.mall.modules.marketing.vo.UserCouponVO;
import com.pzhu.mall.modules.marketing.entity.Promotion;
import com.pzhu.mall.security.LoginUserContext;
import com.pzhu.mall.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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

    @Resource
    private UserCouponMapper userCouponMapper;

    @Resource
    private CouponMapper couponMapper;

    @Resource
    private PromotionMapper promotionMapper;

    // ==================== 消费者端 ====================

    @Operation(summary = "可领取的优惠券列表（消费者）")
    @RequireRole(1)
    @GetMapping("/coupons/available")
    public Result<List<Coupon>> available() {
        Long userId = LoginUserContext.getCurrentUserId();
        return Result.success(couponService.listAvailable(userId));
    }

    @Operation(summary = "领取优惠券")
    @RequireRole(1)
    @PostMapping("/coupons/{id}/receive")
    public Result<java.util.Map<String, Long>> receive(@PathVariable Long id) {
        Long userId = LoginUserContext.getCurrentUserId();
        couponService.receive(userId, id);
        return Result.success(java.util.Map.of("userCouponId", id));
    }

    @Operation(summary = "我的优惠券列表（消费者）")
    @RequireRole(1)
    @GetMapping("/user/coupons")
    public Result<List<UserCouponVO>> userCoupons(@RequestParam(required = false) Integer status) {
        Long userId = LoginUserContext.getCurrentUserId();
        List<UserCoupon> userCoupons = couponService.listUserCoupons(userId, status);

        List<UserCouponVO> vos = userCoupons.stream().map(uc -> {
            Coupon coupon = couponMapper.selectById(uc.getCouponId());
            UserCouponVO vo = new UserCouponVO();
            vo.setId(uc.getId());
            vo.setCouponId(uc.getCouponId());
            vo.setStatus(uc.getStatus());
            vo.setUseTime(uc.getUseTime());
            vo.setRelatedOrderId(uc.getRelatedOrderId());
            vo.setCreateTime(uc.getCreateTime());
            if (coupon != null) {
                vo.setName(coupon.getName());
                vo.setType(coupon.getType() != null ? coupon.getType().toString() : null);
                vo.setDiscountRule(coupon.getDiscountRule());
                vo.setValidFrom(coupon.getValidFrom());
                vo.setValidTo(coupon.getValidTo());
            }
            return vo;
        }).collect(Collectors.toList());

        return Result.success(vos);
    }

    @Operation(summary = "进行中促销活动列表")
    @GetMapping("/promotions/active")
    public Result<List<Promotion>> activePromotions(
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) Long scopeId) {
        List<Promotion> promotions;
        if (scope != null && scopeId != null) {
            // 按 scope 和 scopeId 精确查询
            promotions = promotionMapper.selectList(
                    new LambdaQueryWrapper<Promotion>()
                            .eq(Promotion::getStatus, 1)
                            .eq(Promotion::getScope, scope)
                            .eq(Promotion::getScopeId, scopeId)
                            .ge(Promotion::getStartTime, LocalDateTime.now().minusDays(1))
                            .le(Promotion::getEndTime, LocalDateTime.now().plusDays(1))
            );
        } else {
            promotions = promotionService.listActive();
        }
        return Result.success(promotions);
    }

    // ==================== 管理员端 ====================

    @Operation(summary = "优惠券管理列表")
    @RequireRole(3)
    @GetMapping("/admin/coupons")
    public Result<List<Coupon>> adminList() {
        return Result.success(couponService.listAll());
    }

    @Operation(summary = "创建优惠券")
    @RequireRole(3)
    @PostMapping("/admin/coupons")
    public Result<Void> create(@RequestBody Coupon coupon) {
        couponService.create(coupon);
        return Result.success();
    }

    @Operation(summary = "更新优惠券")
    @RequireRole(3)
    @PutMapping("/admin/coupons/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Coupon coupon) {
        coupon.setId(id);
        couponService.update(coupon);
        return Result.success();
    }

    @Operation(summary = "提前下线优惠券")
    @RequireRole(3)
    @PutMapping("/admin/coupons/{id}/offline")
    public Result<Void> offline(@PathVariable Long id) {
        couponService.offline(id);
        return Result.success();
    }

    @Operation(summary = "删除优惠券")
    @RequireRole(3)
    @DeleteMapping("/admin/coupons/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        couponService.delete(id);
        return Result.success();
    }

    @Operation(summary = "创建促销活动")
    @RequireRole(3)
    @PostMapping("/admin/promotions")
    public Result<Void> createPromotion(@RequestBody Promotion promotion) {
        promotionService.create(promotion);
        return Result.success();
    }

    @Operation(summary = "更新促销活动")
    @RequireRole(3)
    @PutMapping("/admin/promotions/{id}")
    public Result<Void> updatePromotion(@PathVariable Long id, @RequestBody Promotion promotion) {
        promotion.setId(id);
        promotionService.update(promotion);
        return Result.success();
    }

    @Operation(summary = "提前下线促销活动")
    @RequireRole(3)
    @PutMapping("/admin/promotions/{id}/offline")
    public Result<Void> offlinePromotion(@PathVariable Long id) {
        promotionService.offline(id);
        return Result.success();
    }

    @Operation(summary = "删除促销活动")
    @RequireRole(3)
    @DeleteMapping("/admin/promotions/{id}")
    public Result<Void> deletePromotion(@PathVariable Long id) {
        promotionService.delete(id);
        return Result.success();
    }
}
