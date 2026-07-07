package com.pzhu.mall.modules.statistics.task;

import com.pzhu.mall.modules.statistics.service.MerchantStatisticsService;
import com.pzhu.mall.modules.statistics.service.PlatformStatisticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 数据统计定时预计算任务（每日凌晨执行）。
 * <p>
 * 将前一日/累计指标预计算并写入缓存，避免看板接口实时聚合明细表。
 */
@Component
public class StatisticsSnapshotTask {

    private static final Logger log = LoggerFactory.getLogger(StatisticsSnapshotTask.class);

    private final PlatformStatisticsService platformStatisticsService;
    private final MerchantStatisticsService merchantStatisticsService;

    public StatisticsSnapshotTask(PlatformStatisticsService platformStatisticsService,
                                  MerchantStatisticsService merchantStatisticsService) {
        this.platformStatisticsService = platformStatisticsService;
        this.merchantStatisticsService = merchantStatisticsService;
    }

    /**
     * 每日凌晨 1:00 预计算平台看板指标。
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void preComputePlatformDashboard() {
        log.info("[统计任务] 开始预计算平台看板指标");
        try {
            var data = platformStatisticsService.getDashboard();
            log.info("[统计任务] 平台看板预计算完成: GMV={}, 订单数={}", data.get("gmv"), data.get("orderCount"));
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
