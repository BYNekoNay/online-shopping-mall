package com.pzhu.mall.modules.recommend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pzhu.mall.common.config.RedisKeyPrefix;
import com.pzhu.mall.modules.product.entity.Product;
import com.pzhu.mall.modules.product.mapper.ProductMapper;
import com.pzhu.mall.modules.recommend.entity.RecommendResult;
import com.pzhu.mall.modules.recommend.mapper.RecommendResultMapper;
import com.pzhu.mall.modules.recommend.vo.RecommendVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 推荐服务（查询层）。
 *
 * <p>查询优先级：Redis Sorted Set → recommend_result 表 → 热门兜底。
 */
@Service
public class RecommendService {

    private static final Logger log = LoggerFactory.getLogger(RecommendService.class);

    private static final String RECOMMEND_ZSET_KEY_PREFIX = RedisKeyPrefix.RECOMMEND + ":";
    private static final String HOT_PRODUCTS_KEY = RedisKeyPrefix.RECOMMEND + ":hot:products";
    private static final int DEFAULT_RECOMMEND_NUM = 10;

    @Resource
    private RecommendResultMapper recommendResultMapper;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 猜你喜欢（优先 Redis → 数据库 → 热门兜底）。
     */
    public List<RecommendVO> guessYouLike(Long userId, Integer num) {
        int limit = num != null ? num : DEFAULT_RECOMMEND_NUM;

        // 1. 优先读 Redis Sorted Set
        if (userId != null) {
            List<RecommendVO> redisResults = readFromRedis(userId, limit);
            if (!redisResults.isEmpty()) {
                log.info("[推荐-猜你喜欢] 用户={} 命中Redis缓存，返回{}条", userId, redisResults.size());
                return redisResults;
            }
        }

        // 2. 未登录或无缓存：读 recommend_result 表
        List<RecommendResult> dbResults;
        if (userId != null) {
            dbResults = recommendResultMapper.selectList(
                    new LambdaQueryWrapper<RecommendResult>()
                            .eq(RecommendResult::getUserId, userId)
                            .orderByDesc(RecommendResult::getScore)
                            .last("LIMIT " + limit)
            );
        } else {
            // 未登录：全局热门兜底（user_id IS NULL）
            dbResults = recommendResultMapper.selectList(
                    new LambdaQueryWrapper<RecommendResult>()
                            .isNull(RecommendResult::getUserId)
                            .orderByDesc(RecommendResult::getScore)
                            .last("LIMIT " + limit)
            );
        }

        if (!dbResults.isEmpty()) {
            List<Long> productIds = dbResults.stream()
                    .map(RecommendResult::getProductId)
                    .collect(Collectors.toList());
            List<Product> products = productMapper.selectBatchIds(productIds);
            List<RecommendVO> vos = toRecommendVOList(products, dbResults);
            // 回写 Redis
            if (userId != null) {
                writeToRedis(userId, dbResults, limit);
            }
            log.info("[推荐-猜你喜欢] 用户={} 命中数据库，返回{}条", userId, vos.size());
            return vos;
        }

        // 3. 数据库无结果：返回热门商品兜底
        log.info("[推荐-猜你喜欢] 用户={} 无推荐结果，使用热门兜底", userId);
        List<Product> hotProducts = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, 1)
                        .orderByDesc(Product::getSales)
                        .last("LIMIT " + limit)
        );
        return hotProducts.stream()
                .map(p -> RecommendVO.from(p, 0.0, 4))
                .collect(Collectors.toList());
    }

    /**
     * 相似商品推荐。
     *
     * <p>查询 recommend_result 表中 product_id = 目标商品 的记录（按 score 排序），
     * 这些记录由 RecommendCalculateService 的 ItemCF 计算后写入（algorithm_type=2 或 3）。
     */
    public List<RecommendVO> similar(Long productId, Integer num) {
        int limit = num != null ? num : DEFAULT_RECOMMEND_NUM;

        List<RecommendResult> results = recommendResultMapper.selectList(
                new LambdaQueryWrapper<RecommendResult>()
                        .eq(RecommendResult::getProductId, productId)
                        .orderByDesc(RecommendResult::getScore)
                        .last("LIMIT " + limit)
        );

        if (results.isEmpty()) {
            // 兜底：同分类热门商品
            Product current = productMapper.selectById(productId);
            if (current != null) {
                log.info("[推荐-相似商品] 商品={} 无相似结果，使用同分类热门兜底", productId);
                return productMapper.selectList(
                        new LambdaQueryWrapper<Product>()
                                .eq(Product::getCategoryId, current.getCategoryId())
                                .ne(Product::getId, productId)
                                .eq(Product::getStatus, 1)
                                .orderByDesc(Product::getSales)
                                .last("LIMIT " + limit)
                ).stream()
                        .map(p -> RecommendVO.from(p, 0.0, 4))
                        .collect(Collectors.toList());
            }
            log.info("[推荐-相似商品] 商品={} 不存在，返回空列表", productId);
            return List.of();
        }

        List<Long> productIds = results.stream()
                .map(RecommendResult::getProductId)
                .collect(Collectors.toList());
        List<Product> products = productMapper.selectBatchIds(productIds);
        log.info("[推荐-相似商品] 商品={} 命中{}条相似结果", productId, results.size());
        return toRecommendVOList(products, results);
    }

    // ==================== Redis 读写 ====================

    /**
     * 从 Redis Sorted Set 读取推荐结果。
     */
    private List<RecommendVO> readFromRedis(Long userId, int limit) {
        String key = RECOMMEND_ZSET_KEY_PREFIX + userId;
        Set<String> members = stringRedisTemplate.opsForZSet()
                .reverseRange(key, 0, limit - 1);
        if (members == null || members.isEmpty()) {
            return List.of();
        }
        List<Long> productIds = members.stream()
                .map(Long::parseLong)
                .collect(Collectors.toList());
        List<Product> products = productMapper.selectBatchIds(productIds);
        // 重建 score 映射（Redis 中 score 即为排序依据）
        List<Long> finalProductIds = productIds;
        return products.stream()
                .map(p -> {
                    // 用商品在列表中的逆序位置作为近似 score（越靠前分数越高）
                    int idx = finalProductIds.indexOf(p.getId());
                    double score = idx >= 0 ? (double) (limit - idx) / limit : 0.0;
                    return RecommendVO.from(p, score, 3);
                })
                .collect(Collectors.toList());
    }

    /**
     * 将推荐结果写入 Redis Sorted Set。
     */
    private void writeToRedis(Long userId, List<RecommendResult> results, int limit) {
        if (userId == null || results.isEmpty()) {
            return;
        }
        String key = RECOMMEND_ZSET_KEY_PREFIX + userId;
        // 先清空旧数据
        stringRedisTemplate.delete(key);
        // 写入 Sorted Set（score 为推荐分数）
        for (RecommendResult r : results) {
            if (r.getScore() != null) {
                stringRedisTemplate.opsForZSet().add(key, String.valueOf(r.getProductId()), r.getScore().doubleValue());
            }
        }
        // 设置过期时间（24 小时，与定时任务周期匹配）
        stringRedisTemplate.expire(key, java.time.Duration.ofHours(24));
        log.info("[推荐-缓存] 用户={} 写入Redis Sorted Set {}条，过期24h", userId, results.size());
    }

    /**
     * 写入热门商品 Redis 缓存。
     */
    public void cacheHotProducts() {
        List<Product> hotProducts = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, 1)
                        .orderByDesc(Product::getSales)
                        .last("LIMIT 50")
        );
        stringRedisTemplate.delete(HOT_PRODUCTS_KEY);
        for (int i = 0; i < hotProducts.size(); i++) {
            stringRedisTemplate.opsForZSet().add(
                    HOT_PRODUCTS_KEY,
                    String.valueOf(hotProducts.get(i).getId()),
                    (double) (hotProducts.size() - i)
            );
        }
        stringRedisTemplate.expire(HOT_PRODUCTS_KEY, java.time.Duration.ofHours(24));
        log.info("[推荐-缓存] 热门商品缓存写入{}条", hotProducts.size());
    }

    // ==================== 工具方法 ====================

    /**
     * 将 Product 列表与 RecommendResult 的 score/algorithmType 映射为 RecommendVO。
     */
    private List<RecommendVO> toRecommendVOList(List<Product> products, List<RecommendResult> results) {
        Map<Long, RecommendResult> scoreMap = results.stream()
                .collect(Collectors.toMap(RecommendResult::getProductId, r -> r));
        return products.stream()
                .map(p -> {
                    RecommendResult r = scoreMap.get(p.getId());
                    double score = r != null && r.getScore() != null ? r.getScore().doubleValue() : 0.0;
                    int algoType = r != null && r.getAlgorithmType() != null ? r.getAlgorithmType() : 3;
                    return RecommendVO.from(p, score, algoType);
                })
                .collect(Collectors.toList());
    }
}
