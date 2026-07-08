package com.pzhu.mall.modules.marketing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.modules.marketing.entity.Coupon;
import com.pzhu.mall.modules.marketing.entity.UserCoupon;
import com.pzhu.mall.modules.marketing.mapper.CouponMapper;
import com.pzhu.mall.modules.marketing.mapper.UserCouponMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 优惠券服务。
 */
@Service
public class CouponService {

    private static final Logger log = LoggerFactory.getLogger(CouponService.class);

    @Resource
    private CouponMapper couponMapper;

    @Resource
    private UserCouponMapper userCouponMapper;

    /**
     * 用户领取优惠券。
     *
     * <p>乐观更新防超发：{@code UPDATE coupon SET received_count = received_count + 1
     * WHERE id=? AND received_count < stock}。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long receive(Long userId, Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null || coupon.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (LocalDateTime.now().isAfter(coupon.getValidTo())) {
            throw new BusinessException(ErrorCode.COUPON_EXPIRED);
        }
        if (coupon.getReceivedCount() >= coupon.getStock()) {
            throw new BusinessException(ErrorCode.COUPON_SOLD_OUT);
        }

        // 乐观更新防超发
        int updated = couponMapper.update(null,
                new LambdaUpdateWrapper<Coupon>()
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
        log.info("[优惠券] 用户={} 领取优惠券={} 成功 userCouponId={}", userId, couponId, uc.getId());
        return uc.getId();
    }

    /**
     * 计算优惠券抵扣金额。
     *
     * <p>按 discount_rule JSON 解析，支持满减模式。
     */
    public BigDecimal calculateDiscount(Long couponId, BigDecimal goodsAmount) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null || coupon.getIsDeleted() == 1) {
            return BigDecimal.ZERO;
        }
        if (LocalDateTime.now().isAfter(coupon.getValidTo())) {
            return BigDecimal.ZERO;
        }
        if (coupon.getDiscountRule() == null || coupon.getDiscountRule().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.Map<String, Object> rule = mapper.readValue(coupon.getDiscountRule(), java.util.Map.class);
            int threshold = ((Number) rule.getOrDefault("threshold", 0)).intValue();
            int discount = ((Number) rule.getOrDefault("discount", 0)).intValue();
            if (goodsAmount.compareTo(new BigDecimal(threshold)) >= 0) {
                return new BigDecimal(discount);
            }
        } catch (Exception e) {
            log.warn("[优惠券] 解析 discount_rule 失败 couponId={}", couponId, e);
        }
        return BigDecimal.ZERO;
    }

    /**
     * 计算优惠券抵扣金额（传入 discountRule JSON 字符串，避免重复查库）。
     */
    public BigDecimal calculateDiscount(String discountRule, BigDecimal goodsAmount) {
        if (discountRule == null || discountRule.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.Map<String, Object> rule = mapper.readValue(discountRule, java.util.Map.class);
            int threshold = ((Number) rule.getOrDefault("threshold", 0)).intValue();
            int discount = ((Number) rule.getOrDefault("discount", 0)).intValue();
            if (goodsAmount.compareTo(new BigDecimal(threshold)) >= 0) {
                return new BigDecimal(discount);
            }
        } catch (Exception e) {
            log.warn("[优惠券] 解析 discount_rule 失败 rule={}", discountRule, e);
        }
        return BigDecimal.ZERO;
    }

    /**
     * 标记优惠券已使用。
     */
    @Transactional(rollbackFor = Exception.class)
    public void markUsed(Long userCouponId, Long orderId) {
        UserCoupon uc = new UserCoupon();
        uc.setStatus(1);
        uc.setUseTime(LocalDateTime.now());
        uc.setRelatedOrderId(orderId);
        LambdaUpdateWrapper<UserCoupon> uw = new LambdaUpdateWrapper<>();
        uw.eq(UserCoupon::getId, userCouponId)
          .eq(UserCoupon::getStatus, 0)
          .last("LIMIT 1");
        userCouponMapper.update(uc, uw);
    }

    /**
     * 用户可用优惠券列表（按状态过滤）。
     */
    public List<UserCoupon> listUserCoupons(Long userId, Integer status) {
        LambdaQueryWrapper<UserCoupon> qw = new LambdaQueryWrapper<>();
        qw.eq(UserCoupon::getUserId, userId);
        if (status != null) {
            qw.eq(UserCoupon::getStatus, status);
        }
        qw.orderByDesc(UserCoupon::getCreateTime);
        return userCouponMapper.selectList(qw);
    }

    /**
     * 当前用户可领取的优惠券列表（排除已领取、已过期、已下线）。
     */
    public List<Coupon> listAvailable(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        // 查询所有有效优惠券模板
        LambdaQueryWrapper<Coupon> qw = new LambdaQueryWrapper<>();
        qw.eq(Coupon::getIsDeleted, 0)
          .gt(Coupon::getValidTo, now)
          .apply("stock > received_count")
          .orderByDesc(Coupon::getCreateTime);
        List<Coupon> all = couponMapper.selectList(qw);

        // 排除用户已领取的
        List<UserCoupon> received = userCouponMapper.selectList(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
        );
        java.util.Set<Long> receivedIds = received.stream()
                .map(UserCoupon::getCouponId)
                .collect(java.util.stream.Collectors.toSet());

        return all.stream()
                .filter(c -> !receivedIds.contains(c.getId()))
                .toList();
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
        log.info("[优惠券] 管理员创建优惠券 name={} type={}", coupon.getName(), coupon.getType());
    }

    /**
     * 更新优惠券（管理端）。
     */
    public void update(Coupon coupon) {
        couponMapper.updateById(coupon);
        log.info("[优惠券] 管理员更新优惠券 id={}", coupon.getId());
    }

    /**
     * 提前下线优惠券（stock 置为 received_count，使其不可再被领取）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void offline(Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        coupon.setStock(coupon.getReceivedCount());
        couponMapper.updateById(coupon);
        log.info("[优惠券] 管理员下线优惠券 id={}", couponId);
    }

    /**
     * 删除优惠券（软删除）。
     */
    public void delete(Long couponId) {
        Coupon coupon = new Coupon();
        coupon.setId(couponId);
        coupon.setIsDeleted(1);
        couponMapper.updateById(coupon);
        log.info("[优惠券] 管理员删除优惠券 id={}", couponId);
    }

    /**
     * 释放优惠券（订单取消时调用）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void releaseByOrderId(Long orderId) {
        LambdaUpdateWrapper<UserCoupon> uw = new LambdaUpdateWrapper<>();
        uw.eq(UserCoupon::getRelatedOrderId, orderId)
          .eq(UserCoupon::getStatus, 1)
          .set(UserCoupon::getStatus, 0)
          .set(UserCoupon::getUseTime, null)
          .set(UserCoupon::getRelatedOrderId, null);
        userCouponMapper.update(null, uw);
        log.info("[优惠券] 订单={} 取消，释放关联优惠券", orderId);
    }

    /**
     * 根据 ID 查询优惠券。
     */
    public Coupon getById(Long couponId) {
        return couponMapper.selectById(couponId);
    }
}
