package com.pzhu.mall.modules.behavior.controller;

import com.pzhu.mall.modules.behavior.dto.BehaviorRecordDTO;
import com.pzhu.mall.modules.behavior.dto.RecommendClickDTO;
import com.pzhu.mall.modules.behavior.dto.RecommendExposureDTO;
import com.pzhu.mall.modules.behavior.service.BehaviorService;
import com.pzhu.mall.security.LoginUserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * BehaviorController 单元测试（行为埋点/曝光/点击落库 BE-02）。
 */
class BehaviorControllerTest {

    private BehaviorService behaviorService;
    private BehaviorController controller;

    @BeforeEach
    void setUp() {
        behaviorService = mock(BehaviorService.class);
        controller = new BehaviorController();
        inject(controller, "behaviorService", behaviorService);
    }

    @AfterEach
    void tearDown() {
        LoginUserContext.clear();
    }

    @Test
    void record_delegates() {
        LoginUserContext.set(100L, 1);
        BehaviorRecordDTO dto = new BehaviorRecordDTO();
        dto.setBehaviorType(1);
        dto.setProductId(1L);

        var result = controller.record(dto);

        assertNotNull(result);
        verify(behaviorService).record(any(), any(), any());
    }

    @Test
    void recommendExposure_delegates() {
        LoginUserContext.set(100L, 1);
        RecommendExposureDTO dto = new RecommendExposureDTO();
        dto.setSource("home-guess");
        dto.setProductIds(java.util.Collections.singletonList(1L));

        var result = controller.recommendExposure(dto);

        assertNotNull(result);
        verify(behaviorService).recordRecommendExposure(dto);
    }

    @Test
    void recommendClick_delegates() {
        LoginUserContext.set(100L, 1);
        RecommendClickDTO dto = new RecommendClickDTO();
        dto.setProductId(1L);
        dto.setSource("home-guess");

        var result = controller.recommendClick(dto);

        assertNotNull(result);
        verify(behaviorService).recordRecommendClick(dto);
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
