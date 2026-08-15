package com.pzhu.mall.modules.marketing.service;

import com.pzhu.mall.modules.marketing.entity.Promotion;
import com.pzhu.mall.modules.marketing.mapper.PromotionMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.math.RoundingMode;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    // ==================== M-01 满赠 matchGift 用例 ====================

    private PromotionService svcWithActive(List<Promotion> promotions) {
        PromotionMapper mapper = mock(PromotionMapper.class);
        when(mapper.selectList(any())).thenReturn(promotions);
        PromotionService s = new PromotionService();
        ReflectionTestUtils.setField(s, "promotionMapper", mapper);
        return s;
    }

    private Promotion type3Promo(String ruleJson) {
        Promotion p = new Promotion();
        p.setId(1L);
        p.setType(3);
        p.setStatus(1);
        p.setIsDeleted(0);
        p.setStartTime(LocalDateTime.now().minusDays(1));
        p.setEndTime(LocalDateTime.now().plusDays(1));
        p.setRuleJson(ruleJson);
        return p;
    }

    @Test
    void matchGift_thresholdMet_returnsGiftInfo() {
        PromotionService s = svcWithActive(List.of(
                type3Promo("{\"threshold\":150,\"giftProductId\":5001,\"giftSkuId\":50011,\"giftQuantity\":2}")));
        PromotionService.GiftInfo gift = s.matchGift(1L, new BigDecimal("160"));
        assertNotNull(gift);
        assertEquals(5001L, gift.giftProductId());
        assertEquals(50011L, gift.giftSkuId());
        assertEquals(2, gift.giftQuantity());
    }

    @Test
    void matchGift_thresholdNotMet_returnsNull() {
        PromotionService s = svcWithActive(List.of(
                type3Promo("{\"threshold\":150,\"giftProductId\":5001,\"giftSkuId\":50011,\"giftQuantity\":1}")));
        assertNull(s.matchGift(1L, new BigDecimal("100")));
    }

    @Test
    void matchGift_missingGiftSkuId_returnsNull() {
        PromotionService s = svcWithActive(List.of(
                type3Promo("{\"threshold\":150,\"giftProductId\":5001}")));
        assertNull(s.matchGift(1L, new BigDecimal("200")));
    }

    @Test
    void matchGift_invalidJson_returnsNull() {
        PromotionService s = svcWithActive(List.of(type3Promo("not-json")));
        assertNull(s.matchGift(1L, new BigDecimal("200")));
    }

    @Test
    void matchGift_nonType3_returnsNull() {
        Promotion p = type3Promo("{\"threshold\":150,\"giftProductId\":5001,\"giftSkuId\":50011}");
        p.setType(2);
        PromotionService s = svcWithActive(List.of(p));
        assertNull(s.matchGift(1L, new BigDecimal("200")));
    }

    // ==================== A-2 促销范围收紧（validatePromotion 经 create 触发） ====================

    private Promotion validShopPromotion() {
        Promotion p = new Promotion();
        p.setName("测试促销");
        p.setType(1);
        p.setScope("SHOP");
        p.setScopeId(1L);
        p.setStartTime(LocalDateTime.now().minusDays(1));
        p.setEndTime(LocalDateTime.now().plusDays(1));
        p.setRuleJson("{\"discountPercent\":0.8}");
        return p;
    }

    @Test
    void create_scopeShop_valid() {
        // P-01：scope=SHOP + scopeId 正常 → 创建成功（promotionMapper.insert 被调用）
        PromotionMapper mapper = mock(PromotionMapper.class);
        PromotionService s = new PromotionService();
        ReflectionTestUtils.setField(s, "promotionMapper", mapper);
        Promotion p = validShopPromotion();
        s.create(p);
        org.mockito.Mockito.verify(mapper).insert(p);
    }

    @Test
    void create_scopeProduct_rejected() {
        // P-02：scope=PRODUCT → 抛 40001
        PromotionService s = new PromotionService();
        Promotion p = validShopPromotion();
        p.setScope("PRODUCT");
        com.pzhu.mall.common.exception.BusinessException ex =
                assertThrows(com.pzhu.mall.common.exception.BusinessException.class, () -> s.create(p));
        assertTrue(ex.getMessage().contains("仅支持 SHOP"));
    }

    @Test
    void create_scopeCategory_rejected() {
        // P-03：scope=CATEGORY → 抛 40001
        PromotionService s = new PromotionService();
        Promotion p = validShopPromotion();
        p.setScope("CATEGORY");
        assertThrows(com.pzhu.mall.common.exception.BusinessException.class, () -> s.create(p));
    }

    @Test
    void create_scopeNull_rejected() {
        // P-04：scope 为空 → 抛 40001
        PromotionService s = new PromotionService();
        Promotion p = validShopPromotion();
        p.setScope(null);
        assertThrows(com.pzhu.mall.common.exception.BusinessException.class, () -> s.create(p));
    }

    @Test
    void create_scopeIdNull_rejected() {
        // P-05：scopeId 为空 → 抛 40001（A-2 新增校验）
        PromotionService s = new PromotionService();
        Promotion p = validShopPromotion();
        p.setScopeId(null);
        com.pzhu.mall.common.exception.BusinessException ex =
                assertThrows(com.pzhu.mall.common.exception.BusinessException.class, () -> s.create(p));
        assertTrue(ex.getMessage().contains("scopeId"));
    }

    @Test
    void update_scopeProduct_rejected() {
        // P-06：update 同校验 → scope=PRODUCT 抛 40001
        PromotionService s = new PromotionService();
        Promotion p = validShopPromotion();
        p.setScope("PRODUCT");
        assertThrows(com.pzhu.mall.common.exception.BusinessException.class, () -> s.update(p));
    }
}
