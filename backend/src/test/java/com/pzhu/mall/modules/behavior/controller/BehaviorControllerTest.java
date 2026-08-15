package com.pzhu.mall.modules.behavior.controller;

import com.pzhu.mall.modules.behavior.dto.BehaviorRecordDTO;
import com.pzhu.mall.modules.behavior.mapper.UserBehaviorMapper;
import com.pzhu.mall.modules.behavior.service.BehaviorService;
import com.pzhu.mall.modules.product.mapper.ProductMapper;
import com.pzhu.mall.security.LoginUserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * BehaviorController 单元测试（E-1 覆盖率补测：behavior.controller 22% → ≥70%）。
 * <p>覆盖 BC-01~BC-04：行为记录（登录/匿名）与匿名浏览。</p>
 */
class BehaviorControllerTest {

    private BehaviorService behaviorService;
    private UserBehaviorMapper userBehaviorMapper;
    private ProductMapper productMapper;
    private BehaviorController controller;

    @BeforeEach
    void setUp() {
        behaviorService = mock(BehaviorService.class);
        userBehaviorMapper = mock(UserBehaviorMapper.class);
        productMapper = mock(ProductMapper.class);
        controller = new BehaviorController();
        inject(controller, "behaviorService", behaviorService);
        inject(controller, "userBehaviorMapper", userBehaviorMapper);
        inject(controller, "productMapper", productMapper);
    }

    @AfterEach
    void tearDown() {
        LoginUserContext.clear();
    }

    @Test
    void record_loggedIn_delegates() {
        // BC-01：登录用户记录行为 → 委托 service（userId 取自登录态）
        LoginUserContext.set(100L, 1);
        BehaviorRecordDTO dto = new BehaviorRecordDTO();
        dto.setProductId(10L);
        dto.setBehaviorType(1);

        controller.record(dto);

        verify(behaviorService).record(100L, 10L, 1);
    }

    @Test
    void record_anonymous_skips() {
        // BC-02：匿名不记录（防伪造 userId）
        BehaviorRecordDTO dto = new BehaviorRecordDTO();
        dto.setProductId(10L);
        dto.setBehaviorType(1);

        controller.record(dto);

        verify(behaviorService, never()).record(anyLong(), anyLong(), anyInt());
    }

    @Test
    void pageEnter_anonymous_returnsSuccess() {
        // BC-03：匿名浏览埋点 → 成功（不抛错）
        var dto = new com.pzhu.mall.modules.behavior.dto.PageViewDTO();
        dto.setPagePath("/product/10");

        var result = controller.pageEnter(dto);

        assertEquals(0, result.getCode());
    }

    @Test
    void recommendClick_loggedIn_forcesUserIdFromLogin() {
        // BC-04：推荐位点击（登录）→ userId 强制取登录态（防伪造）
        LoginUserContext.set(100L, 1);
        var dto = new com.pzhu.mall.modules.behavior.dto.RecommendClickDTO();
        dto.setProductId(10L);
        dto.setUserId(999L); // 伪造的 userId

        controller.recommendClick(dto);

        verify(behaviorService).recordRecommendClick(argThat(c -> c.getUserId() == 100L && c.getProductId() == 10L));
    }

    @Test
    void recommendExposure_delegates() {
        // BC-05：推荐位曝光
        LoginUserContext.set(100L, 1);
        var dto = new com.pzhu.mall.modules.behavior.dto.RecommendExposureDTO();
        dto.setSource("home-guess");

        controller.recommendExposure(dto);

        verify(behaviorService).recordRecommendExposure(dto);
    }

    @Test
    void pageLeave_delegates() {
        // BC-06：页面离开回填
        var dto = new com.pzhu.mall.modules.behavior.dto.PageLeaveDTO();
        dto.setStayDuration(30);

        controller.pageLeave(5L, dto);

        verify(behaviorService).recordPageLeave(5L, 30);
    }

    @Test
    void favorites_returnsList() {
        // BC-07：我的收藏列表（登录）
        LoginUserContext.set(100L, 1);
        var fav = new com.pzhu.mall.modules.behavior.vo.FavoriteVO();
        fav.setProductId(10L);
        when(userBehaviorMapper.selectList(any())).thenReturn(java.util.Collections.emptyList());
        when(productMapper.selectBatchIds(any())).thenReturn(java.util.Collections.emptyList());

        var result = controller.favorites();

        assertEquals(0, result.getCode());
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
