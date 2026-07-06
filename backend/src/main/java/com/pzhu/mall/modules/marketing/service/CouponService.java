package com.pzhu.mall.modules.marketing.service;

import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.modules.marketing.entity.Coupon;
import com.pzhu.mall.modules.marketing.entity.UserCoupon;
import com.pzhu.mall.modules.marketing.mapper.CouponMapper;
import com.pzhu.mall.modules.marketing.mapper.UserCouponMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 优惠券服务。
 */
@Service
public class CouponService {

    @Resource
    private CouponMapper couponMapper;

    @Resource
    private UserCouponMapper userCouponMapper;

    /**
     * 用户领取优惠券。
     */
    public void receive(Long userId, Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (coupon.getReceivedCount() >= coupon.getStock()) {
            throw new BusinessException(ErrorCode.COUPON_SOLD_OUT);
        }
        if (LocalDateTime.now().isAfter(coupon.getValidTo())) {
            throw new BusinessException(ErrorCode.COUPON_EXPIRED);
        }

        // 乐观更新防止超发
        int updated = couponMapper.update(null,
            new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Coupon>()
                .set(Coupon::getReceivedCount, coupon.getReceivedCount() + 1)
                .eq(Coupon::getId, couponId)
                .eq(Coupon::getReceivedCount, coupon.getReceivedCount())
        );
        if (updated == 0) {
            throw new BusinessException(ErrorCode.COUPON_SOLD_OUT);
        }

        UserCoupon uc = new UserCoupon();
        uc.setUserId(userId);
        uc.setCouponId(couponId);
        uc.setStatus(0);
        uc.setCreateTime(LocalDateTime.now());
        userCouponMapper.insert(uc);
    }

    /**
     * 计算优惠券抵扣金额。
     */
    public java.math.BigDecimal calculateDiscount(Long couponId, java.math.BigDecimal goodsAmount) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            return java.math.BigDecimal.ZERO;
        }
        // 简化计算：按 discount_rule JSON 解析
        // discount_rule 示例：{"threshold":100,"discount":20} 满100减20
        if (coupon.getDiscountRule() == null || coupon.getDiscountRule().isEmpty()) {
            return java.math.BigDecimal.ZERO;
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.Map<String, Object> rule = mapper.readValue(coupon.getDiscountRule(), java.util.Map.class);
            int threshold = ((Number) rule.getOrDefault("threshold", 0)).intValue();
            int discount = ((Number) rule.getOrDefault("discount", 0)).intValue();
            if (goodsAmount.compareTo(new java.math.BigDecimal(threshold)) >= 0) {
                return new java.math.BigDecimal(discount);
            }
        } catch (Exception e) {
            // ignore
        }
        return java.math.BigDecimal.ZERO;
    }

    /**
     * 标记优惠券已使用。
     */
    public void markUsed(Long couponId, Long orderId) {
        UserCoupon uc = new UserCoupon();
        uc.setStatus(1);
        uc.setUseTime(LocalDateTime.now());
        uc.setRelatedOrderId(orderId);
        var uw = new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<UserCoupon>();
        uw.eq(UserCoupon::getCouponId, couponId).eq(UserCoupon::getStatus, 0).last("LIMIT 1");
        userCouponMapper.update(uc, uw);
    }

    /**
     * 用户可用优惠券列表。
     */
    public List<Coupon> listAvailable(Long userId) {
        // 简单返回所有未过期的优惠券模板
        return couponMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Coupon>()
                .gt(Coupon::getValidTo, LocalDateTime.now())
                .orderByDesc(Coupon::getCreateTime)
        );
    }

    /**
     * 所有优惠券列表（管理端）。
     */
    public List<Coupon> listAll() {
        return couponMapper.selectList(null);
    }

    /**
     * 创建优惠券（管理端）。
     */
    public void create(Coupon coupon) {
        couponMapper.insert(coupon);
    }
}
