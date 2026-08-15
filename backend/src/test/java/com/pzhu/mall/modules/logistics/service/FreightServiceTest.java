package com.pzhu.mall.modules.logistics.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.pzhu.mall.modules.logistics.entity.FreightTemplate;
import com.pzhu.mall.modules.logistics.mapper.FreightTemplateMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * FreightService 单元测试（运费计算：多模板/免邮/大区规则/默认）。
 * <p>覆盖 docs/32 批次2 的 L-T01~06 用例：LG-01 模板选择、LG-02 入参校验。</p>
 */
class FreightServiceTest {

    private FreightTemplateMapper freightTemplateMapper;
    private FreightService service;

    @BeforeAll
    static void initTableInfo() {
        if (!TableInfoHelper.getTableInfos().stream()
                .anyMatch(t -> t.getEntityType() == FreightTemplate.class)) {
            TableInfoHelper.initTableInfo(
                    new MapperBuilderAssistant(new MybatisConfiguration(), ""), FreightTemplate.class);
        }
    }

    @BeforeEach
    void setUp() {
        freightTemplateMapper = mock(FreightTemplateMapper.class);
        service = new FreightService();
        inject(service, "freightTemplateMapper", freightTemplateMapper);
    }

    @Test
    void calculate_noTemplate_returnsZero() {
        // 未配置模板默认免运费
        when(freightTemplateMapper.selectList(any())).thenReturn(Collections.emptyList());

        assertEquals(BigDecimal.ZERO, service.calculate(1L, "广东", new BigDecimal("100")));
    }

    @Test
    void calculate_freeShippingThresholdMet_returnsZero() {
        // 满额免邮：金额 ≥ threshold → 0
        FreightTemplate t = new FreightTemplate();
        t.setId(1L);
        t.setShopId(1L);
        t.setFreeShippingThreshold(new BigDecimal("99"));
        t.setDefaultFee(new BigDecimal("10"));
        when(freightTemplateMapper.selectList(any())).thenReturn(Collections.singletonList(t));

        assertEquals(BigDecimal.ZERO, service.calculate(1L, "广东", new BigDecimal("100")));
    }

    @Test
    void calculate_freeShippingThresholdNotMet_chargesDefault() {
        FreightTemplate t = new FreightTemplate();
        t.setId(1L);
        t.setShopId(1L);
        t.setFreeShippingThreshold(new BigDecimal("199"));
        t.setDefaultFee(new BigDecimal("10"));
        when(freightTemplateMapper.selectList(any())).thenReturn(Collections.singletonList(t));

        assertEquals(new BigDecimal("10"), service.calculate(1L, "广东", new BigDecimal("100")));
    }

    @Test
    void calculate_regionRuleMatch_returnsRegionFee() {
        // 广东→华南 → 规则匹配 8 元
        FreightTemplate t = new FreightTemplate();
        t.setId(1L);
        t.setShopId(1L);
        t.setRegionRuleJson("[{\"region\":\"华南\",\"fee\":8}]");
        t.setDefaultFee(new BigDecimal("15"));
        when(freightTemplateMapper.selectList(any())).thenReturn(Collections.singletonList(t));

        assertEquals(new BigDecimal("8"), service.calculate(1L, "广东", new BigDecimal("50")));
    }

    @Test
    void calculate_regionRuleNoMatch_returnsDefault() {
        // 东北无规则 → 默认 15
        FreightTemplate t = new FreightTemplate();
        t.setId(1L);
        t.setShopId(1L);
        t.setRegionRuleJson("[{\"region\":\"华南\",\"fee\":8}]");
        t.setDefaultFee(new BigDecimal("15"));
        when(freightTemplateMapper.selectList(any())).thenReturn(Collections.singletonList(t));

        assertEquals(new BigDecimal("15"), service.calculate(1L, "黑龙江", new BigDecimal("50")));
    }

    @Test
    void calculate_unknownProvince_usesDefault() {
        FreightTemplate t = new FreightTemplate();
        t.setId(1L);
        t.setShopId(1L);
        t.setRegionRuleJson("[{\"region\":\"华南\",\"fee\":8}]");
        t.setDefaultFee(new BigDecimal("12"));
        when(freightTemplateMapper.selectList(any())).thenReturn(Collections.singletonList(t));

        // "未知省" 不在映射 → region=default → 无匹配 → 默认 12
        assertEquals(new BigDecimal("12"), service.calculate(1L, "未知省", new BigDecimal("50")));
    }

    @Test
    void calculate_invalidRuleJson_logsAndFallsBack() {
        FreightTemplate t = new FreightTemplate();
        t.setId(1L);
        t.setShopId(1L);
        t.setRegionRuleJson("not-json");
        t.setDefaultFee(new BigDecimal("10"));
        when(freightTemplateMapper.selectList(any())).thenReturn(Collections.singletonList(t));

        assertEquals(new BigDecimal("10"), service.calculate(1L, "广东", new BigDecimal("50")));
    }

    @Test
    void calculate_ordersByAscUsesFirstTemplate() {
        // LG-01 修复验证：多模板按 id 升序取最先创建（wrapper 需 orderByAsc + LIMIT 1）
        FreightTemplate t = new FreightTemplate();
        t.setId(5L);
        t.setShopId(1L);
        t.setDefaultFee(new BigDecimal("10"));
        when(freightTemplateMapper.selectList(any())).thenReturn(Collections.singletonList(t));

        service.calculate(1L, "广东", new BigDecimal("50"));

        var captor = org.mockito.ArgumentCaptor.forClass(
                com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class);
        verify(freightTemplateMapper).selectList(captor.capture());
        assertTrue(captor.getValue().getSqlSegment().toLowerCase().contains("order by"));
    }

    @Test
    void listByShop_returnsTemplates() {
        when(freightTemplateMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<FreightTemplate> result = service.listByShop(1L);

        assertNotNull(result);
        verify(freightTemplateMapper).selectList(any());
    }

    @Test
    void getById_delegates() {
        when(freightTemplateMapper.selectById(1L)).thenReturn(new FreightTemplate());

        assertNotNull(service.getById(1L));
    }

    private static void inject(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
