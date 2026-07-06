package com.pzhu.mall.modules.recommend.task;

import com.pzhu.mall.modules.recommend.service.RecommendCalculateService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 推荐结果定时刷新任务。
 */
@Component
public class RecommendRefreshTask {

    private final RecommendCalculateService recommendCalculateService;

    public RecommendRefreshTask(RecommendCalculateService recommendCalculateService) {
        this.recommendCalculateService = recommendCalculateService;
    }

    /**
     * 每天凌晨 2 点刷新推荐结果。
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void refresh() {
        recommendCalculateService.calculateForAll();
    }
}
