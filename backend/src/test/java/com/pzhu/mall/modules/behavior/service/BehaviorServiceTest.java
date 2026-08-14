package com.pzhu.mall.modules.behavior.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.modules.behavior.dto.PageViewDTO;
import com.pzhu.mall.modules.behavior.dto.RecommendClickDTO;
import com.pzhu.mall.modules.behavior.dto.RecommendExposureDTO;
import com.pzhu.mall.modules.behavior.entity.PageViewLog;
import com.pzhu.mall.modules.behavior.entity.RecommendExposureLog;
import com.pzhu.mall.modules.behavior.entity.UserBehavior;
import com.pzhu.mall.modules.behavior.mapper.PageViewLogMapper;
import com.pzhu.mall.modules.behavior.mapper.RecommendExposureLogMapper;
import com.pzhu.mall.modules.behavior.mapper.UserBehaviorMapper;
import com.pzhu.mall.security.LoginUserContext;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * BehaviorService 单元测试。
 * <p>覆盖 M-26（页面进入 userId 只取登录态）与 M-27（页面离开归属校验 + 停留时长封顶）。
 * 纯 Mockito，不启动 Spring 上下文。</p>
 */
class BehaviorServiceTest {

    private UserBehaviorMapper userBehaviorMapper;
    private PageViewLogMapper pageViewLogMapper;
    private RecommendExposureLogMapper recommendExposureLogMapper;
    private BehaviorService service;

    @BeforeAll
    static void initTableInfo() {
        var assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, UserBehavior.class);
        TableInfoHelper.initTableInfo(assistant, RecommendExposureLog.class);
    }

    @BeforeEach
    void setUp() {
        userBehaviorMapper = mock(UserBehaviorMapper.class);
        pageViewLogMapper = mock(PageViewLogMapper.class);
        recommendExposureLogMapper = mock(RecommendExposureLogMapper.class);
        service = new BehaviorService();
        inject(service, "userBehaviorMapper", userBehaviorMapper);
        inject(service, "pageViewLogMapper", pageViewLogMapper);
        inject(service, "recommendExposureLogMapper", recommendExposureLogMapper);
    }

    @AfterEach
    void tearDown() {
        LoginUserContext.clear();
    }

    // ==================== M-26 页面进入 userId 取登录态 ====================

    @Test
    void recordPageEnter_usesLoginUserNotDto() {
        // M-26 修复验证：即使前端伪造 dto.userId=999，也以登录态 100L 入库
        LoginUserContext.set(100L, 1);
        PageViewDTO dto = new PageViewDTO();
        dto.setUserId(999L);
        dto.setSessionId("sess-1");
        dto.setPagePath("/product/10");

        service.recordPageEnter(dto);

        ArgumentCaptor<PageViewLog> captor = ArgumentCaptor.forClass(PageViewLog.class);
        verify(pageViewLogMapper).insert(captor.capture());
        assertEquals(100L, captor.getValue().getUserId());
        assertEquals("/product/10", captor.getValue().getPagePath());
    }

    @Test
    void recordPageEnter_anonymousStoresNullUserId() {
        // 未登录访客：userId 为 null（匿名），不信任前端传入值
        PageViewDTO dto = new PageViewDTO();
        dto.setUserId(999L);
        dto.setPagePath("/home");

        service.recordPageEnter(dto);

        ArgumentCaptor<PageViewLog> captor = ArgumentCaptor.forClass(PageViewLog.class);
        verify(pageViewLogMapper).insert(captor.capture());
        assertNull(captor.getValue().getUserId());
    }

    // ==================== M-27 页面离开归属校验 + 时长封顶 ====================

    @Test
    void recordPageLeave_owner_updates() {
        LoginUserContext.set(100L, 1);
        PageViewLog existing = new PageViewLog();
        existing.setId(1L);
        existing.setUserId(100L);
        when(pageViewLogMapper.selectById(1L)).thenReturn(existing);

        assertDoesNotThrow(() -> service.recordPageLeave(1L, 120));

        ArgumentCaptor<PageViewLog> captor = ArgumentCaptor.forClass(PageViewLog.class);
        verify(pageViewLogMapper).updateById(captor.capture());
        assertEquals(120, captor.getValue().getStayDuration());
        assertNotNull(captor.getValue().getLeaveTime());
    }

    @Test
    void recordPageLeave_otherUsersLog_throwsForbidden() {
        // M-27 修复验证：登录用户 100L 不能回填他人（999L）的页面日志
        LoginUserContext.set(100L, 1);
        PageViewLog existing = new PageViewLog();
        existing.setId(1L);
        existing.setUserId(999L);
        when(pageViewLogMapper.selectById(1L)).thenReturn(existing);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.recordPageLeave(1L, 120));
        assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
        verify(pageViewLogMapper, never()).updateById(any());
    }

    @Test
    void recordPageLeave_anonymousUpdatingOwnedLog_throwsForbidden() {
        // 未登录调用者不能回填带 userId 的日志
        PageViewLog existing = new PageViewLog();
        existing.setId(1L);
        existing.setUserId(999L);
        when(pageViewLogMapper.selectById(1L)).thenReturn(existing);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.recordPageLeave(1L, 120));
        assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    void recordPageLeave_anonymousLog_updatedByAnonymous() {
        // 匿名日志（userId=null）允许匿名调用者回填
        PageViewLog existing = new PageViewLog();
        existing.setId(1L);
        existing.setUserId(null);
        when(pageViewLogMapper.selectById(1L)).thenReturn(existing);

        assertDoesNotThrow(() -> service.recordPageLeave(1L, 30));
        verify(pageViewLogMapper).updateById(any());
    }

    @Test
    void recordPageLeave_capsExcessiveDuration() {
        // M-27 修复验证：超过 24h 的停留时长封顶为 86400 秒
        LoginUserContext.set(100L, 1);
        PageViewLog existing = new PageViewLog();
        existing.setId(1L);
        existing.setUserId(100L);
        when(pageViewLogMapper.selectById(1L)).thenReturn(existing);

        service.recordPageLeave(1L, Integer.MAX_VALUE);

        ArgumentCaptor<PageViewLog> captor = ArgumentCaptor.forClass(PageViewLog.class);
        verify(pageViewLogMapper).updateById(captor.capture());
        assertEquals(86400, captor.getValue().getStayDuration());
    }

    @Test
    void recordPageLeave_negativeDuration_clampedToZero() {
        LoginUserContext.set(100L, 1);
        PageViewLog existing = new PageViewLog();
        existing.setId(1L);
        existing.setUserId(100L);
        when(pageViewLogMapper.selectById(1L)).thenReturn(existing);

        service.recordPageLeave(1L, -50);

        ArgumentCaptor<PageViewLog> captor = ArgumentCaptor.forClass(PageViewLog.class);
        verify(pageViewLogMapper).updateById(captor.capture());
        assertEquals(0, captor.getValue().getStayDuration());
    }

    @Test
    void recordPageLeave_logNotFound_noop() {
        when(pageViewLogMapper.selectById(1L)).thenReturn(null);
        assertDoesNotThrow(() -> service.recordPageLeave(1L, 120));
        verify(pageViewLogMapper, never()).updateById(any());
    }

    // ==================== BE-02 曝光/点击落库 ====================

    @Test
    void recordRecommendExposure_persistsPerProduct_clickedZero() {
        // BE-02 修复验证：曝光逐商品落库（clicked=0），供 CTR 统计
        RecommendExposureDTO dto = new RecommendExposureDTO();
        dto.setUserId(100L);
        dto.setSource("home-guess");
        dto.setProductIds(List.of(10L, 20L, 30L));

        service.recordRecommendExposure(dto);

        ArgumentCaptor<RecommendExposureLog> captor = ArgumentCaptor.forClass(RecommendExposureLog.class);
        verify(recommendExposureLogMapper, times(3)).insert(captor.capture());
        assertEquals(3, captor.getAllValues().size());
        assertEquals(0, captor.getAllValues().get(0).getClicked());
        assertEquals(10L, captor.getAllValues().get(0).getProductId());
        assertEquals("home-guess", captor.getAllValues().get(0).getSource());
        assertEquals(100L, captor.getAllValues().get(0).getUserId());
    }

    @Test
    void recordRecommendClick_marksExposureClicked() {
        // BE-02 修复验证：点击回标最近一条未点击曝光（clicked 0→1），无曝光时插入 clicked=1
        RecommendClickDTO dto = new RecommendClickDTO();
        dto.setUserId(100L);
        dto.setSource("home-guess");
        dto.setProductId(10L);
        dto.setPosition(1);
        when(recommendExposureLogMapper.update(any(), any())).thenReturn(1);

        service.recordRecommendClick(dto);

        // 曝光被回标（未走插入分支）
        verify(recommendExposureLogMapper).update(any(), any());
        verify(recommendExposureLogMapper, never()).insert(any());
    }

    @Test
    void recordRecommendClick_noExposure_insertsClicked() {
        // BE-02 修复验证：无曝光记录时插入 clicked=1
        RecommendClickDTO dto = new RecommendClickDTO();
        dto.setUserId(100L);
        dto.setSource("product-similar");
        dto.setProductId(10L);
        dto.setPosition(2);
        when(recommendExposureLogMapper.update(any(), any())).thenReturn(0);

        service.recordRecommendClick(dto);

        ArgumentCaptor<RecommendExposureLog> captor = ArgumentCaptor.forClass(RecommendExposureLog.class);
        verify(recommendExposureLogMapper).insert(captor.capture());
        assertEquals(1, captor.getValue().getClicked());
        assertEquals(10L, captor.getValue().getProductId());
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
