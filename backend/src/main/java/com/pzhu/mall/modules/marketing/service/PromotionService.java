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
                        yield goodsAmount.subtract(new BigDecimal(priceObj.toString()));
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
     */
    @Transactional(rollbackFor = Exception.class)
    public void create(Promotion promotion) {
        promotionMapper.insert(promotion);
        log.info("[促销] 管理员创建促销 name={} type={} scope={}", promotion.getName(), promotion.getType(), promotion.getScope());
    }

    /**
     * 更新促销活动（管理端）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(Promotion promotion) {
        promotionMapper.updateById(promotion);
        log.info("[促销] 管理员更新促销 id={}", promotion.getId());
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
