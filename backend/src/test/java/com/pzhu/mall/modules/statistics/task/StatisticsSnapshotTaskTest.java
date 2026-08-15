package com.pzhu.mall.modules.statistics.task;

import com.pzhu.mall.modules.statistics.service.MerchantStatisticsService;
import com.pzhu.mall.modules.statistics.service.PlatformStatisticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * StatisticsSnapshotTask 单元测试（E-1 覆盖率补测：statistics.task 0% → ≥80%）。
 */
class StatisticsSnapshotTaskTest {

    private PlatformStatisticsService platformStatisticsService;
    private MerchantStatisticsService merchantStatisticsService;
    private StringRedisTemplate stringRedisTemplate;
    private StatisticsSnapshotTask task;

    @BeforeEach
    void setUp() {
        platformStatisticsService = mock(PlatformStatisticsService.class);
        merchantStatisticsService = mock(MerchantStatisticsService.class);
        stringRedisTemplate = mock(StringRedisTemplate.class);
        task = new StatisticsSnapshotTask(platformStatisticsService, merchantStatisticsService, stringRedisTemplate);
    }

    @Test
    void preComputePlatformDashboard_success_writesCache() throws Exception {
        // SS-01：预计算成功 → JSON 写入 Redis
        Map<String, Object> data = new HashMap<>();
        data.put("gmv", "10000.00");
        data.put("orderCount", 50);
        when(platformStatisticsService.getDashboard()).thenReturn(data);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);

        task.preComputePlatformDashboard();

        verify(valueOps).set(eq(StatisticsSnapshotTask.DASHBOARD_CACHE_KEY), contains("10000.00"), any());
    }

    @Test
    void preComputePlatformDashboard_serviceError_doesNotWriteCache() {
        // SS-02：服务异常 → 不写缓存（异常吞掉，不影响主流程）
        when(platformStatisticsService.getDashboard()).thenThrow(new RuntimeException("boom"));
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);

        task.preComputePlatformDashboard();

        verify(valueOps, never()).set(anyString(), anyString(), any());
    }
}
