package com.pzhu.mall.modules.marketing.controller;

import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.modules.marketing.entity.Coupon;
import com.pzhu.mall.modules.marketing.entity.UserCoupon;
import com.pzhu.mall.modules.marketing.mapper.UserCouponMapper;
import com.pzhu.mall.modules.marketing.mapper.CouponMapper;
import com.pzhu.mall.modules.marketing.service.CouponService;
import com.pzhu.mall.modules.marketing.vo.UserCouponVO;
import com.pzhu.mall.security.LoginUserContext;
import com.pzhu.mall.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
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
    private UserCouponMapper userCouponMapper;

    @Resource
    private CouponMapper couponMapper;

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
        Long userCouponId = couponService.receive(userId, id);
        return Result.success(java.util.Map.of("userCouponId", userCouponId));
    }

    @Operation(summary = "我的优惠券列表（消费者）")
    @RequireRole(1)
    @GetMapping("/user/coupons")
    public Result<List<UserCouponVO>> userCoupons(@RequestParam(required = false) Integer status) {
        Long userId = LoginUserContext.getCurrentUserId();
        List<UserCoupon> userCoupons = couponService.listUserCoupons(userId, status);

        // M-05 修复：批量加载 Coupon 模板，消除循环 selectById 的 N+1
        java.util.Set<Long> couponIds = userCoupons.stream()
                .map(UserCoupon::getCouponId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        java.util.Map<Long, Coupon> couponMap = new java.util.HashMap<>();
        if (!couponIds.isEmpty()) {
            couponMapper.selectBatchIds(couponIds)
                    .forEach(c -> couponMap.put(c.getId(), c));
        }

        List<UserCouponVO> vos = userCoupons.stream().map(uc -> {
            Coupon coupon = couponMap.get(uc.getCouponId());
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

}
