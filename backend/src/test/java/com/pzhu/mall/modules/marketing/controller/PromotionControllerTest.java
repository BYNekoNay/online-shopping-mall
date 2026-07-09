package com.pzhu.mall.modules.marketing.controller;

import com.pzhu.mall.modules.marketing.entity.Promotion;
import com.pzhu.mall.modules.marketing.mapper.PromotionMapper;
import com.pzhu.mall.modules.marketing.service.PromotionService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PromotionControllerTest {

    @Test
    void active_returnsPromotionList() {
        PromotionService promotionService = mock(PromotionService.class);
        PromotionMapper promotionMapper = mock(PromotionMapper.class);
        PromotionController controller = new PromotionController();
        inject(controller, "promotionService", promotionService);
        inject(controller, "promotionMapper", promotionMapper);

        List<Promotion> mockList = List.of(new Promotion());
        when(promotionService.listActive()).thenReturn(mockList);

        var result = controller.active(null, null);
        assertEquals(mockList, result.getData());
    }

    @Test
    void active_withScopeId_filtersCorrectly() {
        PromotionService promotionService = mock(PromotionService.class);
        PromotionMapper promotionMapper = mock(PromotionMapper.class);
        PromotionController controller = new PromotionController();
        inject(controller, "promotionService", promotionService);
        inject(controller, "promotionMapper", promotionMapper);

        var result = controller.active("SHOP", 1L);
        assertNotNull(result.getData());
    }

    @Test
    void active_withoutScopeId_fallsBack() {
        PromotionService promotionService = mock(PromotionService.class);
        PromotionMapper promotionMapper = mock(PromotionMapper.class);
        PromotionController controller = new PromotionController();
        inject(controller, "promotionService", promotionService);
        inject(controller, "promotionMapper", promotionMapper);

        List<Promotion> mockList = List.of(new Promotion());
        when(promotionService.listActive()).thenReturn(mockList);

        var result = controller.active("SHOP", null);
        assertEquals(mockList, result.getData());
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
