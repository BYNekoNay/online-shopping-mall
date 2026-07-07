package com.pzhu.mall.modules.recommend.task;

import com.pzhu.mall.modules.recommend.service.RecommendCalculateService;
import com.pzhu.mall.modules.recommend.service.RecommendService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 推荐结果定时刷新任务。
 *
 * <p>每天凌晨 2 点全量重算推荐结果，并刷新热门商品 Redis 缓存。
 */
@Component
public class RecommendRefreshTask {

    private static final Logger log = LoggerFactory.getLogger(RecommendRefreshTask.class);

    private final RecommendCalculateService recommendCalculateService;

    private final RecommendService recommendService;

    public RecommendRefreshTask(RecommendCalculateService recommendCalculateService,
                                RecommendService recommendService) {
        this.recommendCalculateService = recommendCalculateService;
        this.recommendService = recommendService;
    }

    /**
     * 每天凌晨 2 点刷新推荐结果。
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void refresh() {
        log.info("[推荐-定时任务] 开始执行推荐结果刷新");
        try {
            recommendCalculateService.calculateForAll();
            recommendService.cacheHotProducts();
            log.info("[推荐-定时任务] 推荐结果刷新完成");
        } catch (Exception e) {
            log.error("[推荐-定时任务] 推荐结果刷新失败", e);
        }
    }
}
