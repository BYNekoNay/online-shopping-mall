package com.pzhu.mall.modules.marketing.service;

import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.modules.marketing.entity.Promotion;
import com.pzhu.mall.modules.marketing.mapper.PromotionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 促销活动服务。
 */
@Service
public class PromotionService {

    private static final Logger log = LoggerFactory.getLogger(PromotionService.class);
    private static final com.fasterxml.jackson.databind.ObjectMapper OBJECT_MAPPER = new com.fasterxml.jackson.databind.ObjectMapper();

    @Resource
    private PromotionMapper promotionMapper;

    /**
     * 计算店铺促销折扣金额（取同类型最大优惠）。
     */
    public BigDecimal calculateDiscount(Long shopId, BigDecimal goodsAmount) {
        List<Promotion> promotions = matchActive(shopId);
        BigDecimal maxDiscount = BigDecimal.ZERO;
        for (Promotion p : promotions) {
            BigDecimal discount = calculateSingleDiscount(p, goodsAmount);
            if (discount != null && discount.compareTo(maxDiscount) > 0) {
                maxDiscount = discount;
            }
        }
        return maxDiscount;
    }

    /**
     * 根据促销类型计算单条促销的折扣金额。
     */
    BigDecimal calculateSingleDiscount(Promotion p, BigDecimal goodsAmount) {
        if (p.getType() == null || p.getRuleJson() == null || p.getRuleJson().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            java.util.Map<String, Object> rule = OBJECT_MAPPER.readValue(p.getRuleJson(), java.util.Map.class);
            return switch (p.getType()) {
                case 1 -> { // 限时折扣：ruleJson={"discountPercent":0.8}
                    double percent = ((Number) rule.getOrDefault("discountPercent", 1.0)).doubleValue();
                    // M10 修复：校验折扣比例范围（0.01~0.99），防止异常配置
                    if (percent <= 0.0 || percent >= 1.0) {
                        log.warn("[促销] 折扣比例异常 promotionId={} discountPercent={}", p.getId(), percent);
                        yield BigDecimal.ZERO;
                    }
                    BigDecimal pct = BigDecimal.valueOf(percent);
                    yield goodsAmount.multiply(BigDecimal.ONE.subtract(pct));
                }
                case 2 -> { // 满减：ruleJson={"threshold":100,"reduce":20}
                    int threshold = ((Number) rule.getOrDefault("threshold", 0)).intValue();
                    int reduce = ((Number) rule.getOrDefault("reduce", 0)).intValue();
                    if (goodsAmount.compareTo(new BigDecimal(threshold)) >= 0) {
                        yield new BigDecimal(reduce);
                    }
                    yield BigDecimal.ZERO;
                }
                case 3 -> BigDecimal.ZERO; // 满赠：无金额减免，仅返回是否达到门槛
                case 4 -> { // 组合套餐：ruleJson={"packagePrice":299}
                    Object priceObj = rule.get("packagePrice");
                    if (priceObj != null) {
                        // H-14 修复：套餐价高于商品金额时折扣为负，会把订单金额越减越大，需与 0 取较大值
                        BigDecimal diff = goodsAmount.subtract(new BigDecimal(priceObj.toString()));
                        yield diff.compareTo(BigDecimal.ZERO) > 0 ? diff : BigDecimal.ZERO;
                    }
                    yield BigDecimal.ZERO;
                }
                default -> BigDecimal.ZERO;
            };
        } catch (Exception e) {
            log.warn("[促销] 解析 rule_json 失败 promotionId={} rule={}", p.getId(), p.getRuleJson(), e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * 查询店铺当前生效的促销活动。
     */
    public List<Promotion> listActiveByShop(Long shopId) {
        LocalDateTime now = LocalDateTime.now();
        return promotionMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Promotion>()
                        .eq(Promotion::getStatus, 1)
                        .eq(Promotion::getIsDeleted, 0) // H-12 修复：排除已软删除的促销，避免其仍被应用到订单
                        .eq(Promotion::getScope, "SHOP")
                        .eq(Promotion::getScopeId, shopId)
                        .le(Promotion::getStartTime, now)
                        .ge(Promotion::getEndTime, now)
        );
    }

    /**
     * 匹配店铺当前生效的促销活动（供下单流程调用）。
     */
    public List<Promotion> matchActive(Long shopId) {
        return listActiveByShop(shopId);
    }

    /**
     * 满赠信息（type=3 促销命中后的赠品配置）。
     *
     * @param giftProductId 赠品商品 ID
     * @param giftSkuId     赠品 SKU ID（须属于 giftProductId）
     * @param giftQuantity  赠送数量
     */
    public record GiftInfo(Long giftProductId, Long giftSkuId, Integer giftQuantity) {
    }

    /**
     * M-01 修复：匹配店铺当前生效的满赠促销（type=3）。
     * <p>ruleJson = {"threshold":150.00,"giftProductId":5001,"giftSkuId":50011,"giftQuantity":1}，
     * 商品金额（不含运费）达到 threshold 时返回赠品配置；不达标/配置非法/解析失败返回 null（容错）。
     * 满赠不产生金额抵扣（金额计算维持 0），赠品行由下单处理器插入。</p>
     *
     * @param shopId      下单店铺 ID
     * @param goodsAmount 该分组商品金额（不含运费）
     * @return 命中满赠的赠品配置，未命中返回 null
     */
    public GiftInfo matchGift(Long shopId, BigDecimal goodsAmount) {
        if (goodsAmount == null || shopId == null) {
            return null;
        }
        List<Promotion> promotions = listActiveByShop(shopId);
        for (Promotion p : promotions) {
            if (p.getType() == null || p.getType() != 3 || p.getRuleJson() == null || p.getRuleJson().isEmpty()) {
                continue;
            }
            try {
                java.util.Map<String, Object> rule = OBJECT_MAPPER.readValue(p.getRuleJson(), java.util.Map.class);
                Object thresholdObj = rule.getOrDefault("threshold", 0);
                BigDecimal threshold = new BigDecimal(thresholdObj.toString());
                if (goodsAmount.compareTo(threshold) < 0) {
                    continue;
                }
                Object giftProductObj = rule.get("giftProductId");
                Object giftSkuObj = rule.get("giftSkuId");
                if (giftProductObj == null || giftSkuObj == null) {
                    log.warn("[促销] 满赠配置缺少 giftProductId/giftSkuId promotionId={}", p.getId());
                    continue;
                }
                int giftQuantity = ((Number) rule.getOrDefault("giftQuantity", 1)).intValue();
                if (giftQuantity <= 0) {
                    giftQuantity = 1;
                }
                log.info("[促销] 满赠命中 shopId={} 金额={} 赠送 SKU={} ×{}", shopId, goodsAmount, giftSkuObj, giftQuantity);
                return new GiftInfo(Long.valueOf(giftProductObj.toString()), Long.valueOf(giftSkuObj.toString()), giftQuantity);
            } catch (Exception e) {
                log.warn("[促销] 解析满赠 rule_json 失败 promotionId={} rule={}", p.getId(), p.getRuleJson(), e);
            }
        }
        return null;
    }

    /**
     * 查询所有进行中促销（消费者端）。
     */
    public List<Promotion> listActive() {
        LocalDateTime now = LocalDateTime.now();
        return promotionMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Promotion>()
                        .eq(Promotion::getStatus, 1)
                        .eq(Promotion::getIsDeleted, 0)
                        .le(Promotion::getStartTime, now)
                        .ge(Promotion::getEndTime, now)
        );
    }

    /**
     * 查询所有促销（管理端，含已下线/删除）。
     */
    public List<Promotion> listAll() {
        return promotionMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Promotion>()
                        .orderByDesc(Promotion::getCreateTime)
        );
    }

    /**
     * 创建促销活动（管理端）。
     * <p>M-06 修复：入参校验（名称/类型/scope/时间窗/rule_json 合法性）。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void create(Promotion promotion) {
        validatePromotion(promotion);
        promotionMapper.insert(promotion);
        log.info("[促销] 管理员创建促销 name={} type={} scope={}", promotion.getName(), promotion.getType(), promotion.getScope());
    }

    /**
     * 更新促销活动（管理端）。
     * <p>M-06 修复：同 create 校验（部分更新场景仅校验非 null 字段）。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(Promotion promotion) {
        validatePromotion(promotion);
        promotionMapper.updateById(promotion);
        log.info("[促销] 管理员更新促销 id={}", promotion.getId());
    }

    /**
     * M-06 修复：管理端促销入参校验。
     */
    private void validatePromotion(Promotion p) {
        if (p == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "促销信息不能为空");
        }
        if (p.getName() == null || p.getName().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "促销名称不能为空");
        }
        if (p.getName().length() > 50) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "促销名称过长（≤50）");
        }
        if (p.getType() == null || (p.getType() < 1 || p.getType() > 4)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "促销类型仅支持 1=限时折扣/2=满减/3=满赠/4=组合套餐");
        }
        if (p.getScope() == null || p.getScope().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "促销范围 scope 不能为空（PRODUCT/CATEGORY/SHOP）");
        }
        if (p.getStartTime() != null && p.getEndTime() != null
                && !p.getEndTime().isAfter(p.getStartTime())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "活动结束时间必须晚于开始时间");
        }
        if (p.getRuleJson() != null && !p.getRuleJson().isBlank()) {
            try {
                OBJECT_MAPPER.readTree(p.getRuleJson());
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "活动规则 rule_json 必须是合法 JSON");
            }
        }
    }

    /**
     * 提前下线促销活动（status 置为 0）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void offline(Long promotionId) {
        Promotion promotion = new Promotion();
        promotion.setId(promotionId);
        promotion.setStatus(0);
        promotionMapper.updateById(promotion);
        log.info("[促销] 管理员下线促销 id={}", promotionId);
    }

    /**
     * 删除促销活动（软删除）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long promotionId) {
        Promotion promotion = new Promotion();
        promotion.setId(promotionId);
        promotion.setIsDeleted(1);
        promotionMapper.updateById(promotion);
        log.info("[促销] 管理员删除促销 id={}", promotionId);
    }

    /**
     * 根据 ID 查询促销活动。
     */
    public Promotion getById(Long promotionId) {
        return promotionMapper.selectById(promotionId);
    }
}
