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

    private final com.pzhu.mall.modules.behavior.mapper.UserBehaviorMapper userBehaviorMapper;

    public RecommendRefreshTask(RecommendCalculateService recommendCalculateService,
                                RecommendService recommendService,
                                StringRedisTemplate stringRedisTemplate,
                                com.pzhu.mall.modules.behavior.mapper.UserBehaviorMapper userBehaviorMapper) {
        this.recommendCalculateService = recommendCalculateService;
        this.recommendService = recommendService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.userBehaviorMapper = userBehaviorMapper;
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
     * F-4 增量刷新：每天 2:30 增量重算"最近 24h 有行为"用户的推荐结果。
     *
     * <p>与 2:00 的全量重算互补：全量保证正确性（R-7 回退兜底），
     * 增量保证准实时性（行为变化后最快次日生效，无需全量重扫）。</p>
     */
    @Scheduled(cron = "0 30 2 * * ?")
    public void refreshIncremental() {
        log.info("[推荐-增量任务] 开始增量重算最近24h活跃用户");
        try {
            java.time.LocalDateTime since = java.time.LocalDateTime.now().minusHours(24);
            List<com.pzhu.mall.modules.behavior.entity.UserBehavior> recent = userBehaviorMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.pzhu.mall.modules.behavior.entity.UserBehavior>()
                            .ge(com.pzhu.mall.modules.behavior.entity.UserBehavior::getCreateTime, since)
                            .in(com.pzhu.mall.modules.behavior.entity.UserBehavior::getBehaviorType, 1, 2, 3, 4)
                            .last("LIMIT 5000"));
            java.util.Set<Long> userIds = recent.stream()
                    .map(com.pzhu.mall.modules.behavior.entity.UserBehavior::getUserId)
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.toSet());
            if (userIds.isEmpty()) {
                log.info("[推荐-增量任务] 无活跃用户，跳过");
                return;
            }
            for (Long uid : userIds) {
                try {
                    recommendCalculateService.calculateForUser(uid);
                } catch (Exception e) {
                    // R-7 预案：单用户失败不阻断批量，全量任务次日兜底
                    log.warn("[推荐-增量任务] 用户={} 增量计算失败（全量任务兜底）", uid, e);
                }
            }
            log.info("[推荐-增量任务] 增量重算完成，共 {} 个活跃用户", userIds.size());
        } catch (Exception e) {
            log.error("[推荐-增量任务] 执行失败（全量任务次日兜底）", e);
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
