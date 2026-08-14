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
        // M-03 修复：每人限领 1 张（与 listAvailable"已领排除"口径一致）。
        // 原实现仅靠列表接口排除已领，receive 接口可被直接调用重复领取，此处后端兜底校验
        Long alreadyCount = userCouponMapper.selectCount(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getCouponId, couponId)
        );
        if (alreadyCount != null && alreadyCount > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "每人限领 1 张，请勿重复领取");
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
     *
     * <p>M-02 修复：使用前校验适用范围——归属、状态（未使用）、有效期、
     * 店铺券（type=4）须匹配下单店铺 shopId、品类券（type=3）须匹配分组内商品品类。
     * 校验不通过返回 0（不抵扣、不阻断下单），避免跨店/跨品类错误折扣。
     *
     * @param userCouponId UserCoupon 记录 ID
     * @param goodsAmount  参与优惠的商品金额
     * @param userId       当前用户 ID（归属校验）
     * @param shopId       下单店铺 ID（店铺券校验，null 时不校验）
     * @param categoryIds  分组内商品品类 ID 列表（品类券校验，null 时不校验）
     */
    public BigDecimal calculateDiscountByUserCoupon(Long userCouponId, BigDecimal goodsAmount,
                                                    Long userId, Long shopId, List<Long> categoryIds) {
        UserCoupon uc = userCouponMapper.selectById(userCouponId);
        if (uc == null) return BigDecimal.ZERO;
        // M-02 修复①：归属校验
        if (userId != null && (uc.getUserId() == null || !uc.getUserId().equals(userId))) {
            return BigDecimal.ZERO;
        }
        // M-02 修复②：状态校验（仅未使用）
        if (uc.getStatus() == null || uc.getStatus() != 0) {
            return BigDecimal.ZERO;
        }
        Coupon coupon = couponMapper.selectById(uc.getCouponId());
        if (coupon == null || coupon.getIsDeleted() == 1) {
            return BigDecimal.ZERO;
        }
        // M-02 修复③：有效期校验
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(coupon.getValidTo())
                || (coupon.getValidFrom() != null && now.isBefore(coupon.getValidFrom()))) {
            return BigDecimal.ZERO;
        }
        // M-02 修复④：店铺券（type=4）须匹配下单店铺
        if (coupon.getType() != null && coupon.getType() == 4 && shopId != null) {
            if (coupon.getShopId() == null || !coupon.getShopId().equals(shopId)) {
                log.warn("[优惠券] 店铺券不适用当前店铺 userCouponId={} couponShopId={} orderShopId={}", userCouponId, coupon.getShopId(), shopId);
                return BigDecimal.ZERO;
            }
        }
        // M-02 修复⑤：品类券（type=3）须匹配分组内商品品类
        if (coupon.getType() != null && coupon.getType() == 3 && categoryIds != null && !categoryIds.isEmpty()) {
            Long ruleCategoryId = parseCategoryId(coupon.getDiscountRule());
            if (ruleCategoryId != null && !categoryIds.contains(ruleCategoryId)) {
                log.warn("[优惠券] 品类券不适用当前品类 userCouponId={} ruleCategoryId={} orderCategories={}", userCouponId, ruleCategoryId, categoryIds);
                return BigDecimal.ZERO;
            }
        }
        return parseAndCalc(coupon.getDiscountRule(), goodsAmount, "userCouponId=" + userCouponId);
    }

    /**
     * 兼容旧调用：不校验适用范围（仅保留归属查询语义，建议使用带校验的新重载）。
     */
    public BigDecimal calculateDiscountByUserCoupon(Long userCouponId, BigDecimal goodsAmount) {
        return calculateDiscountByUserCoupon(userCouponId, goodsAmount, null, null, null);
    }

    /**
     * M-02 修复：解析品类券 discount_rule 内嵌的 categoryId。
     */
    private Long parseCategoryId(String ruleJson) {
        if (ruleJson == null || ruleJson.isEmpty()) return null;
        try {
            java.util.Map<String, Object> rule = MAPPER.readValue(ruleJson, java.util.Map.class);
            Object v = rule.get("categoryId");
            return v == null ? null : Long.valueOf(((Number) v).longValue());
        } catch (Exception e) {
            log.warn("[优惠券] 解析品类券 categoryId 失败 rule={}", ruleJson, e);
            return null;
        }
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
     * <p>M-06 修复：入参校验（名称/类型/库存/有效期/discount_rule JSON 合法性）。</p>
     */
    public void create(Coupon coupon) {
        validateCoupon(coupon);
        couponMapper.insert(coupon);
        log.info("[优惠券] 管理员创建优惠券 name={} type={}", coupon.getName(), coupon.getType());
    }

    /**
     * 更新优惠券（管理端）。
     * <p>M-06 修复：同 create 校验（部分更新场景仅校验非 null 字段）。</p>
     */
    public void update(Coupon coupon) {
        validateCoupon(coupon);
        couponMapper.updateById(coupon);
        log.info("[优惠券] 管理员更新优惠券 id={}", coupon.getId());
    }

    /**
     * M-06 修复：管理端券入参校验（创建必填，更新按非 null 字段校验）。
     */
    private void validateCoupon(Coupon coupon) {
        if (coupon == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "优惠券信息不能为空");
        }
        if (coupon.getName() == null || coupon.getName().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "优惠券名称不能为空");
        }
        if (coupon.getName().length() > 50) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "优惠券名称过长（≤50）");
        }
        if (coupon.getType() == null || (coupon.getType() < 1 || coupon.getType() > 4)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "优惠券类型仅支持 1=新人/2=满减/3=品类/4=店铺");
        }
        if (coupon.getStock() != null && coupon.getStock() < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "库存不能为负数");
        }
        if (coupon.getValidFrom() != null && coupon.getValidTo() != null
                && !coupon.getValidTo().isAfter(coupon.getValidFrom())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "有效期结束时间必须晚于开始时间");
        }
        if (coupon.getDiscountRule() != null && !coupon.getDiscountRule().isBlank()) {
            try {
                MAPPER.readTree(coupon.getDiscountRule());
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "优惠规则 discount_rule 必须是合法 JSON");
            }
        }
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
