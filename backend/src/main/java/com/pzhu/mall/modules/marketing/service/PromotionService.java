package com.pzhu.mall.modules.marketing.service;

import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.modules.marketing.entity.Promotion;
import com.pzhu.mall.modules.marketing.mapper.PromotionMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 促销活动服务。
 */
@Service
public class PromotionService {

    @Resource
    private PromotionMapper promotionMapper;

    /**
     * 计算店铺促销折扣金额。
     */
    public java.math.BigDecimal calculateDiscount(Long shopId, java.math.BigDecimal goodsAmount) {
        List<Promotion> promotions = listActiveByShop(shopId);
        java.math.BigDecimal maxDiscount = java.math.BigDecimal.ZERO;
        for (Promotion p : promotions) {
            // 简化处理：按 rule_json 计算折扣
            if (p.getType() == 1) { // 限时折扣
                // rule_json: {"discountPercent":0.8}
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    java.util.Map<String, Object> rule = mapper.readValue(p.getRuleJson(), java.util.Map.class);
                    double percent = ((Number) rule.getOrDefault("discountPercent", 1.0)).doubleValue();
                    java.math.BigDecimal discount = goodsAmount.multiply(new java.math.BigDecimal(1 - percent));
                    if (discount.compareTo(maxDiscount) > 0) {
                        maxDiscount = discount;
                    }
                } catch (Exception e) {
                    // ignore
                }
            } else if (p.getType() == 2) { // 满减
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    java.util.Map<String, Object> rule = mapper.readValue(p.getRuleJson(), java.util.Map.class);
                    int threshold = ((Number) rule.getOrDefault("threshold", 0)).intValue();
                    int reduce = ((Number) rule.getOrDefault("reduce", 0)).intValue();
                    if (goodsAmount.compareTo(new java.math.BigDecimal(threshold)) >= 0) {
                        java.math.BigDecimal discount = new java.math.BigDecimal(reduce);
                        if (discount.compareTo(maxDiscount) > 0) {
                            maxDiscount = discount;
                        }
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
        }
        return maxDiscount;
    }

    /**
     * 查询店铺当前生效的促销活动。
     */
    public List<Promotion> listActiveByShop(Long shopId) {
        return promotionMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Promotion>()
                .eq(Promotion::getStatus, 1)
                .eq(Promotion::getScope, "SHOP")
                .eq(Promotion::getScopeId, shopId)
                .ge(Promotion::getStartTime, LocalDateTime.now().minusDays(1))
                .le(Promotion::getEndTime, LocalDateTime.now().plusDays(1))
        );
    }

    /**
     * 查询所有进行中促销（消费者端）。
     */
    public List<Promotion> listActive() {
        return promotionMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Promotion>()
                .eq(Promotion::getStatus, 1)
                .ge(Promotion::getStartTime, LocalDateTime.now().minusDays(1))
                .le(Promotion::getEndTime, LocalDateTime.now().plusDays(1))
        );
    }

    /**
     * 创建促销活动（管理端）。
     */
    public void create(Promotion promotion) {
        promotionMapper.insert(promotion);
    }
}
