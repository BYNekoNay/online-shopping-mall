package com.pzhu.mall.modules.recommend.task;

import com.pzhu.mall.modules.recommend.service.RecommendCalculateService;
import com.pzhu.mall.modules.recommend.service.RecommendService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RecommendRefreshTask 单元测试（E-1 覆盖率补测：recommend.task 0% → ≥80%）。
 */
class RecommendRefreshTaskTest {

    private RecommendCalculateService recommendCalculateService;
    private RecommendService recommendService;
    private StringRedisTemplate stringRedisTemplate;
    private com.pzhu.mall.modules.behavior.mapper.UserBehaviorMapper userBehaviorMapper;
    private RecommendRefreshTask task;

    @BeforeEach
    void setUp() {
        recommendCalculateService = mock(RecommendCalculateService.class);
        recommendService = mock(RecommendService.class);
        stringRedisTemplate = mock(StringRedisTemplate.class);
        userBehaviorMapper = mock(com.pzhu.mall.modules.behavior.mapper.UserBehaviorMapper.class);
        task = new RecommendRefreshTask(recommendCalculateService, recommendService, stringRedisTemplate, userBehaviorMapper);
    }

    @Test
    void refresh_success_evictsUserCaches() {
        // RR-01：全量重算成功 → 清理用户推荐缓存（排除 similar/hot）
        Set<String> keys = Set.of("mall:recommend:1", "mall:recommend:2", "mall:recommend:similar:5", "mall:recommend:hot:products");
        when(stringRedisTemplate.keys("mall:recommend:*")).thenReturn(keys);
        when(stringRedisTemplate.delete(anyCollection())).thenReturn(2L);

        task.refresh();

        verify(recommendCalculateService).calculateForAll();
        verify(recommendService).cacheHotProducts();
        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<java.util.Collection<String>> captor =
                org.mockito.ArgumentCaptor.forClass(java.util.Collection.class);
        verify(stringRedisTemplate).delete(captor.capture());
        assertEquals(2, captor.getValue().size());
        assertFalse(captor.getValue().contains("mall:recommend:similar:5"));
        assertFalse(captor.getValue().contains("mall:recommend:hot:products"));
    }

    @Test
    void refresh_noKeys_skipsDelete() {
        // RR-02：无缓存键 → 跳过清理
        when(stringRedisTemplate.keys("mall:recommend:*")).thenReturn(Set.of());

        task.refresh();

        verify(stringRedisTemplate, never()).delete(anyCollection());
    }

    @Test
    void refresh_calculateError_swallowed() {
        // RR-03：计算异常 → 吞掉，不抛（定时任务不中断）
        doThrow(new RuntimeException("calc boom")).when(recommendCalculateService).calculateForAll();

        task.refresh(); // 不抛异常即通过

        verify(recommendService, never()).cacheHotProducts();
    }

    @Test
    void refresh_keysScanError_swallowed() {
        // RR-04：keys 扫描异常 → 吞掉（缓存 24h 自动过期可接受）
        when(stringRedisTemplate.keys("mall:recommend:*")).thenThrow(new RuntimeException("redis boom"));

        task.refresh();

        // 主链路已完成
        verify(recommendCalculateService).calculateForAll();
    }

    // ==================== F-4 增量刷新 ====================

    private static com.pzhu.mall.modules.behavior.entity.UserBehavior behavior(Long userId) {
        com.pzhu.mall.modules.behavior.entity.UserBehavior b =
                new com.pzhu.mall.modules.behavior.entity.UserBehavior();
        b.setUserId(userId);
        b.setBehaviorType(1);
        return b;
    }

    @Test
    void refreshIncremental_activeUsers_recalculated() {
        // RI-01：24h 内活跃用户 → 逐个增量重算（去重）
        when(userBehaviorMapper.selectList(any()))
                .thenReturn(List.of(behavior(1L), behavior(1L), behavior(2L)));

        task.refreshIncremental();

        verify(recommendCalculateService).calculateForUser(1L);
        verify(recommendCalculateService).calculateForUser(2L);
    }

    @Test
    void refreshIncremental_noUsers_skips() {
        // RI-02：无活跃用户 → 跳过
        when(userBehaviorMapper.selectList(any())).thenReturn(java.util.Collections.emptyList());

        task.refreshIncremental();

        verify(recommendCalculateService, never()).calculateForUser(anyLong());
    }

    @Test
    void refreshIncremental_userError_continuesBatch() {
        // RI-03：单用户失败不阻断批量（R-7 全量兜底）
        when(userBehaviorMapper.selectList(any()))
                .thenReturn(List.of(behavior(1L), behavior(2L)));
        doThrow(new RuntimeException("calc boom")).when(recommendCalculateService).calculateForUser(1L);

        task.refreshIncremental();

        verify(recommendCalculateService).calculateForUser(2L); // 后续用户仍计算
    }
}
