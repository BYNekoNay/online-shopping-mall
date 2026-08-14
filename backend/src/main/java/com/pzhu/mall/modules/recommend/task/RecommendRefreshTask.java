package com.pzhu.mall.modules.recommend.task;

import com.pzhu.mall.common.config.RedisKeyPrefix;
import com.pzhu.mall.modules.recommend.service.RecommendCalculateService;
import com.pzhu.mall.modules.recommend.service.RecommendService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

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

    private final StringRedisTemplate stringRedisTemplate;

    public RecommendRefreshTask(RecommendCalculateService recommendCalculateService,
                                RecommendService recommendService,
                                StringRedisTemplate stringRedisTemplate) {
        this.recommendCalculateService = recommendCalculateService;
        this.recommendService = recommendService;
        this.stringRedisTemplate = stringRedisTemplate;
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
            // R-05 修复：全量重算后清理用户推荐 ZSet 缓存（mall:recommend:{userId}），
            // 否则旧缓存最长 24h 陈旧（"准实时"名不副实）。similar/hot 键不在此范围。
            evictUserRecommendCaches();
            log.info("[推荐-定时任务] 推荐结果刷新完成");
        } catch (Exception e) {
            log.error("[推荐-定时任务] 推荐结果刷新失败", e);
        }
    }

    /**
     * R-05 修复：遍历并删除用户推荐 ZSet 缓存键。
     * 键格式为 mall:recommend:{userId}，排除 similar:/hot:products 前缀。
     */
    private void evictUserRecommendCaches() {
        try {
            Set<String> keys = stringRedisTemplate.keys(RedisKeyPrefix.RECOMMEND + ":*");
            if (keys == null || keys.isEmpty()) {
                return;
            }
            List<String> userKeys = keys.stream()
                    .filter(k -> !k.contains(":similar:") && !k.endsWith(":hot:products"))
                    .toList();
            if (!userKeys.isEmpty()) {
                stringRedisTemplate.delete(userKeys);
                log.info("[推荐-定时任务] 清理用户推荐缓存 {} 个键", userKeys.size());
            }
        } catch (Exception e) {
            log.warn("[推荐-定时任务] 清理用户推荐缓存失败（可接受，缓存 24h 自动过期）", e);
        }
    }
}
