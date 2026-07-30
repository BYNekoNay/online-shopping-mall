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

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = new com.fasterxml.jackson.databind.ObjectMapper();

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
        // M-08 修复：校验领取开始时间（validFrom 为 null 视为不限制，兼容历史数据）
        if (coupon.getValidFrom() != null && LocalDateTime.now().isBefore(coupon.getValidFrom())) {
            throw new BusinessException(ErrorCode.COUPON_NOT_STARTED);
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
     * <p>按 discount_rule JSON 解析，支持两种模式：
     * <ul>
     *   <li>满减：{@code {"threshold":100,"discount":20}} —— 满 100 减 20</li>
     *   <li>折扣（M-09 修复新增）：{@code {"threshold":100,"rate":0.8}} —— 满 100 打八折，
     *       rate 口径与促销 discountPercent 一致（0.8 = 八折），抵扣额 = 商品金额 × (1 - rate)</li>
     * </ul>
     */
    public BigDecimal calculateDiscount(Long couponId, BigDecimal goodsAmount) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null || coupon.getIsDeleted() == 1) {
            return BigDecimal.ZERO;
        }
        if (LocalDateTime.now().isAfter(coupon.getValidTo())) {
            return BigDecimal.ZERO;
        }
        return parseAndCalc(coupon.getDiscountRule(), goodsAmount, "couponId=" + couponId);
    }

    /**
     * 计算优惠券抵扣金额（传入 discountRule JSON 字符串，避免重复查库）。
     */
    public BigDecimal calculateDiscount(String discountRule, BigDecimal goodsAmount) {
        return parseAndCalc(discountRule, goodsAmount, "rule=" + discountRule);
    }

    /**
     * M-09 修复：统一的 discount_rule 解析入口，同时支持满减（discount）与折扣率（rate）两种规则，
     * 消除原先两个重载各自复制一份解析逻辑的问题。规则同时含 discount 与 rate 时优先按满减处理。
     *
     * @param ruleJson    discount_rule JSON 字符串
     * @param goodsAmount 参与优惠的商品金额
     * @param logTag      日志标签（定位问题用）
     */
    private BigDecimal parseAndCalc(String ruleJson, BigDecimal goodsAmount, String logTag) {
        if (ruleJson == null || ruleJson.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = MAPPER;
            java.util.Map<String, Object> rule = mapper.readValue(ruleJson, java.util.Map.class);
            int threshold = ((Number) rule.getOrDefault("threshold", 0)).intValue();
            if (goodsAmount.compareTo(new BigDecimal(threshold)) < 0) {
                return BigDecimal.ZERO;
            }
            if (rule.containsKey("discount")) {
                // 满减模式：{"threshold":100,"discount":20}
                int discount = ((Number) rule.get("discount")).intValue();
                return new BigDecimal(discount);
            }
            if (rule.containsKey("rate")) {
                // M-09 修复：折扣率模式：{"threshold":100,"rate":0.8}，rate 为 0~1 的折扣比例
                double rate = ((Number) rule.get("rate")).doubleValue();
                if (rate <= 0 || rate >= 1) {
                    log.warn("[优惠券] 折扣率异常 {} rate={}", logTag, rate);
                    return BigDecimal.ZERO;
                }
                return goodsAmount.multiply(BigDecimal.ONE.subtract(new BigDecimal(String.valueOf(rate))))
                        .setScale(2, java.math.RoundingMode.HALF_UP);
            }
        } catch (Exception e) {
            log.warn("[优惠券] 解析 discount_rule 失败 {}", logTag, e);
        }
        return BigDecimal.ZERO;
    }

    /**
     * 计算优惠券抵扣金额（通过 UserCoupon ID 查询关联的 Coupon 模板）。
     *
     * <p>与 {@link #calculateDiscount(Long, BigDecimal)} 的区别在于
     * 此方法先通过 UserCoupon 记录找到对应的 Coupon 模板。
     */
    public BigDecimal calculateDiscountByUserCoupon(Long userCouponId, BigDecimal goodsAmount) {
        UserCoupon uc = userCouponMapper.selectById(userCouponId);
        if (uc == null) return BigDecimal.ZERO;
        return calculateDiscount(uc.getCouponId(), goodsAmount);
    }

    /**
     * 标记优惠券已使用（校验归属）。
     *
     * @param userCouponId UserCoupon 记录 ID
     * @param orderId      关联订单 ID
     * @param userId       当前用户 ID（用于归属校验）
     */
    @Transactional(rollbackFor = Exception.class)
    public void markUsed(Long userCouponId, Long orderId, Long userId) {
        // 先查询 UserCoupon 记录，校验归属
        UserCoupon uc = userCouponMapper.selectById(userCouponId);
        if (uc == null) {
            throw new BusinessException(ErrorCode.COUPON_UNAVAILABLE);
        }
        if (!uc.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.COUPON_UNAVAILABLE);
        }

        // 原子性标记已使用
        UserCoupon update = new UserCoupon();
        update.setStatus(1);
        update.setUseTime(LocalDateTime.now());
        update.setRelatedOrderId(orderId);
        LambdaUpdateWrapper<UserCoupon> uw = new LambdaUpdateWrapper<>();
        uw.eq(UserCoupon::getId, userCouponId)
          .eq(UserCoupon::getStatus, 0);
        // H-08 修复：检查 UPDATE 影响行数，行数为 0 说明优惠券已被使用（并发重复核销），
        // 抛出异常使订单事务回滚，防止一张优惠券被重复使用
        int updated = userCouponMapper.update(update, uw);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.COUPON_UNAVAILABLE);
        }
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
