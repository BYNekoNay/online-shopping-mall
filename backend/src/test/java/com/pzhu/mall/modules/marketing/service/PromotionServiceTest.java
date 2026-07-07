package com.pzhu.mall.modules.marketing.service;

import com.pzhu.mall.modules.marketing.entity.Promotion;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;

class PromotionServiceTest {

    private final PromotionService service = new PromotionService();

    @Test
    void calculateDiscount_promotionType1_discountPercent() {
        Promotion p = new Promotion();
        p.setType(1);
        p.setRuleJson("{\"discountPercent\":0.8}");

        BigDecimal discount = service.calculateSingleDiscount(p, new BigDecimal("100"));
        assertEquals(0, discount.compareTo(new BigDecimal("20")));
    }

    @Test
    void calculateDiscount_promotionType2_thresholdMet() {
        Promotion p = new Promotion();
        p.setType(2);
        p.setRuleJson("{\"threshold\":100,\"reduce\":20}");

        BigDecimal discount = service.calculateSingleDiscount(p, new BigDecimal("150"));
        assertEquals(new BigDecimal("20"), discount);
    }

    @Test
    void calculateDiscount_promotionType2_thresholdNotMet() {
        Promotion p = new Promotion();
        p.setType(2);
        p.setRuleJson("{\"threshold\":100,\"reduce\":20}");

        BigDecimal discount = service.calculateSingleDiscount(p, new BigDecimal("50"));
        assertEquals(BigDecimal.ZERO, discount);
    }

    @Test
    void calculateDiscount_nullType_returnsZero() {
        Promotion p = new Promotion();
        p.setType(null);

        BigDecimal discount = service.calculateSingleDiscount(p, new BigDecimal("100"));
        assertEquals(BigDecimal.ZERO, discount);
    }

    @Test
    void calculateDiscount_invalidJson_returnsZero() {
        Promotion p = new Promotion();
        p.setType(1);
        p.setRuleJson("not-json");

        BigDecimal discount = service.calculateSingleDiscount(p, new BigDecimal("100"));
        assertEquals(BigDecimal.ZERO, discount);
    }
}
