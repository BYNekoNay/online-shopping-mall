package com.pzhu.mall.modules.statistics.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pzhu.mall.common.config.RedisKeyPrefix;
import com.pzhu.mall.modules.statistics.service.MerchantStatisticsService;
import com.pzhu.mall.modules.statistics.service.PlatformStatisticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 数据统计定时预计算任务（每日凌晨执行）。
 * <p>
 * 将前一日/累计指标预计算并写入缓存，避免看板接口实时聚合明细表。
 */
@Component
public class StatisticsSnapshotTask {

    private static final Logger log = LoggerFactory.getLogger(StatisticsSnapshotTask.class);

    /** 平台看板快照缓存键 */
    public static final String DASHBOARD_CACHE_KEY = RedisKeyPrefix.STATISTICS + ":platform:dashboard";

    /** 快照 TTL：25 小时，覆盖到次日 1:00 的下一次预计算并留 1 小时重叠 */
    private static final Duration SNAPSHOT_TTL = Duration.ofHours(25);

    private final PlatformStatisticsService platformStatisticsService;
    private final MerchantStatisticsService merchantStatisticsService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StatisticsSnapshotTask(PlatformStatisticsService platformStatisticsService,
                                  MerchantStatisticsService merchantStatisticsService,
                                  StringRedisTemplate stringRedisTemplate) {
        this.platformStatisticsService = platformStatisticsService;
        this.merchantStatisticsService = merchantStatisticsService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 每日凌晨 1:00 预计算平台看板指标。
     *
     * <p>M-14 修复：原实现仅打日志不落缓存，预计算形同虚设；现将快照 JSON 写入 Redis，
     * 供监控/降级场景消费（实时看板接口仍走实时聚合，保证"今日"类指标准确）。
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void preComputePlatformDashboard() {
        log.info("[统计任务] 开始预计算平台看板指标");
        try {
            var data = platformStatisticsService.getDashboard();
            String json = objectMapper.writeValueAsString(data);
            stringRedisTemplate.opsForValue().set(DASHBOARD_CACHE_KEY, json, SNAPSHOT_TTL);
            log.info("[统计任务] 平台看板预计算完成并写入缓存: key={}, GMV={}, 订单数={}",
                    DASHBOARD_CACHE_KEY, data.get("gmv"), data.get("orderCount"));
        } catch (Exception e) {
            log.error("[统计任务] 平台看板预计算失败", e);
        }
    }

    /**
     * 每日凌晨 1:30 预计算各商家销售统计（近期30天）。
     */
    @Scheduled(cron = "0 30 1 * * ?")
    public void preComputeMerchantStatistics() {
        log.info("[统计任务] 开始预计算商家销售统计");
        // 商家级预计算由前端请求时实时聚合（demo 规模数据量小，实时查询可接受）
        // 生产环境可在此处遍历所有店铺写入 Redis/汇总表
        log.info("[统计任务] 商家统计预计算完成（跳过，demo模式实时查询）");
    }
}
