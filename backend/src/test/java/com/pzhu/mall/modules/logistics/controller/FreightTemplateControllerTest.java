package com.pzhu.mall.modules.logistics.controller;

import com.pzhu.mall.modules.logistics.entity.FreightTemplate;
import com.pzhu.mall.modules.logistics.service.FreightService;
import com.pzhu.mall.modules.logistics.service.LogisticsQueryService;
import com.pzhu.mall.modules.shop.service.ShopService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * FreightTemplateController 单元测试（运费模板列表/计算）。
 */
class FreightTemplateControllerTest {

    private FreightService freightService;
    private ShopService shopService;
    private LogisticsQueryService logisticsQueryService;
    private com.pzhu.mall.modules.order.mapper.OrderMapper orderMapper;
    private FreightTemplateController controller;

    @BeforeEach
    void setUp() {
        freightService = mock(FreightService.class);
        shopService = mock(ShopService.class);
        logisticsQueryService = mock(LogisticsQueryService.class);
        orderMapper = mock(com.pzhu.mall.modules.order.mapper.OrderMapper.class);
        controller = new FreightTemplateController();
        inject(controller, "freightService", freightService);
        inject(controller, "shopService", shopService);
        inject(controller, "logisticsQueryService", logisticsQueryService);
        inject(controller, "orderMapper", orderMapper);
    }

    @Test
    void merchantList_returnsTemplates() {
        when(freightService.listByShop(anyLong())).thenReturn(Collections.emptyList());

        var result = controller.merchantList();

        assertNotNull(result.getData());
        verify(freightService).listByShop(anyLong());
    }

    @Test
    void calculate_returnsFee() {
        when(freightService.calculate(1L, "广东", new BigDecimal("100")))
                .thenReturn(new BigDecimal("10"));

        var result = controller.calculate(1L, "广东", new BigDecimal("100"));

        assertEquals(new BigDecimal("10"), result.getData());
        verify(freightService).calculate(1L, "广东", new BigDecimal("100"));
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
