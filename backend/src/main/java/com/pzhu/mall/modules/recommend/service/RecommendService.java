package com.pzhu.mall.modules.recommend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import java.util.ArrayList;
import java.util.HashMap;
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
    private static final String SIMILAR_ZSET_KEY_PREFIX = RedisKeyPrefix.RECOMMEND + ":similar:";
    private static final String HOT_PRODUCTS_KEY = RedisKeyPrefix.RECOMMEND + ":hot:products";
    private static final int DEFAULT_RECOMMEND_NUM = 10;

    @Resource
    private RecommendResultMapper recommendResultMapper;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RecommendCalculateService recommendCalculateService;

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
        Page<RecommendResult> page = new Page<>(1, limit);
        if (userId != null) {
            recommendResultMapper.selectPage(page,
                    new LambdaQueryWrapper<RecommendResult>()
                            .eq(RecommendResult::getUserId, userId)
                            .orderByDesc(RecommendResult::getScore)
            );
        } else {
            // 未登录：全局热门兜底（user_id IS NULL）
            recommendResultMapper.selectPage(page,
                    new LambdaQueryWrapper<RecommendResult>()
                            .isNull(RecommendResult::getUserId)
                            .orderByDesc(RecommendResult::getScore)
            );
        }
        dbResults = page.getRecords();

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
        Page<Product> hotPage = new Page<>(1, limit);
        productMapper.selectPage(hotPage,
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, 1)
                        .orderByDesc(Product::getSales)
        );
        return hotPage.getRecords().stream()
                .map(p -> RecommendVO.from(p, 0.0, 4))
                .collect(Collectors.toList());
    }

    /**
     * 相似商品推荐（ItemCF 余弦相似度）。
     *
     * <p>查询优先级：Redis Sorted Set → ItemCF 实时计算 → 同分类热门兜底。
     * 返回与目标商品被同一批用户喜爱的其他商品，而非目标商品自身。
     */
    public List<RecommendVO> similar(Long productId, Integer num) {
        int limit = num != null ? num : DEFAULT_RECOMMEND_NUM;

        // 1. 优先读 Redis 缓存
        List<RecommendVO> cached = readSimilarFromRedis(productId, limit);
        if (!cached.isEmpty()) {
            log.info("[推荐-相似商品] 商品={} 命中Redis缓存，返回{}条", productId, cached.size());
            return cached;
        }

        // 2. ItemCF 实时计算相似商品
        Map<Long, Double> similarScores = recommendCalculateService.computeSimilarProducts(productId, limit);
        if (!similarScores.isEmpty()) {
            List<Long> productIds = new ArrayList<>(similarScores.keySet());
            List<Product> products = productMapper.selectBatchIds(productIds);
            // 过滤下架商品，并按相似度得分构建 VO
            Map<Long, Product> productMap = products.stream()
                    .filter(p -> Integer.valueOf(1).equals(p.getStatus()))
                    .collect(Collectors.toMap(Product::getId, p -> p));
            List<RecommendVO> vos = new ArrayList<>();
            for (Map.Entry<Long, Double> e : similarScores.entrySet()) {
                Product p = productMap.get(e.getKey());
                if (p != null) {
                    vos.add(RecommendVO.from(p, e.getValue(), 2));
                }
            }
            if (!vos.isEmpty()) {
                writeSimilarToRedis(productId, similarScores);
                log.info("[推荐-相似商品] 商品={} ItemCF计算返回{}条", productId, vos.size());
                return vos;
            }
        }

        // 3. 兜底：同分类热门商品
        Product current = productMapper.selectById(productId);
        if (current != null) {
            log.info("[推荐-相似商品] 商品={} 无相似结果，使用同分类热门兜底", productId);
            Page<Product> similarHotPage = new Page<>(1, limit);
            productMapper.selectPage(similarHotPage,
                    new LambdaQueryWrapper<Product>()
                            .eq(Product::getCategoryId, current.getCategoryId())
                            .ne(Product::getId, productId)
                            .eq(Product::getStatus, 1)
                            .orderByDesc(Product::getSales)
            );
            return similarHotPage.getRecords().stream()
                    .map(p -> RecommendVO.from(p, 0.0, 4))
                    .collect(Collectors.toList());
        }
        log.info("[推荐-相似商品] 商品={} 不存在，返回空列表", productId);
        return List.of();
    }

    // ==================== Redis 读写 ====================

    /**
     * 从 Redis Sorted Set 读取推荐结果。
     */
    private List<RecommendVO> readFromRedis(Long userId, int limit) {
        String key = RECOMMEND_ZSET_KEY_PREFIX + userId;
        // R-06 修复：ZSet 已存真实推荐分，用 reverseRangeWithScores 读取真实 score，
        // 替代原"member 顺序 + 位置构造 score"（伪造分数且算法类型写死 3）
        Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<String>> tuples =
                stringRedisTemplate.opsForZSet().reverseRangeWithScores(key, 0, limit - 1);
        if (tuples == null || tuples.isEmpty()) {
            return List.of();
        }
        List<Long> productIds = new ArrayList<>();
        Map<Long, Double> scoreMap = new HashMap<>();
        for (org.springframework.data.redis.core.ZSetOperations.TypedTuple<String> t : tuples) {
            try {
                Long pid = Long.parseLong(t.getValue());
                productIds.add(pid);
                scoreMap.put(pid, t.getScore() != null ? t.getScore() : 0.0);
            } catch (NumberFormatException e) {
                log.warn("[推荐-Redis] 跳过非数字 member: key={}, value={}", key, t.getValue());
            }
        }
        if (productIds.isEmpty()) {
            return List.of();
        }
        List<Product> products = productMapper.selectBatchIds(productIds);
        Map<Long, Product> productMap = products.stream().collect(Collectors.toMap(Product::getId, p -> p));
        List<RecommendVO> vos = new ArrayList<>();
        for (Long pid : productIds) {
            Product p = productMap.get(pid);
            if (p != null && Integer.valueOf(1).equals(p.getStatus())) {
                vos.add(RecommendVO.from(p, scoreMap.getOrDefault(pid, 0.0), 3));
            }
        }
        return vos;
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
     * 从 Redis Sorted Set 读取相似商品缓存。
     */
    private List<RecommendVO> readSimilarFromRedis(Long productId, int limit) {
        String key = SIMILAR_ZSET_KEY_PREFIX + productId;
        Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<String>> tuples =
                stringRedisTemplate.opsForZSet().reverseRangeWithScores(key, 0, limit - 1);
        if (tuples == null || tuples.isEmpty()) {
            return List.of();
        }
        List<Long> productIds = new ArrayList<>();
        Map<Long, Double> scoreMap = new HashMap<>();
        for (org.springframework.data.redis.core.ZSetOperations.TypedTuple<String> t : tuples) {
            try {
                Long pid = Long.parseLong(t.getValue());
                productIds.add(pid);
                scoreMap.put(pid, t.getScore() != null ? t.getScore() : 0.0);
            } catch (NumberFormatException e) {
                log.warn("[推荐-Redis] 跳过非数字 member: key={}, value={}", key, t.getValue());
            }
        }
        if (productIds.isEmpty()) {
            return List.of();
        }
        List<Product> products = productMapper.selectBatchIds(productIds);
        Map<Long, Product> productMap = products.stream().collect(Collectors.toMap(Product::getId, p -> p));
        List<RecommendVO> vos = new ArrayList<>();
        for (Long pid : productIds) {
            Product p = productMap.get(pid);
            if (p != null && Integer.valueOf(1).equals(p.getStatus())) {
                vos.add(RecommendVO.from(p, scoreMap.getOrDefault(pid, 0.0), 2));
            }
        }
        return vos;
    }

    /**
     * 将相似商品结果写入 Redis Sorted Set（24h 过期）。
     */
    private void writeSimilarToRedis(Long productId, Map<Long, Double> scores) {
        String key = SIMILAR_ZSET_KEY_PREFIX + productId;
        stringRedisTemplate.delete(key);
        for (Map.Entry<Long, Double> e : scores.entrySet()) {
            stringRedisTemplate.opsForZSet().add(key, String.valueOf(e.getKey()), e.getValue());
        }
        stringRedisTemplate.expire(key, java.time.Duration.ofHours(24));
    }

    /**
     * 写入热门商品 Redis 缓存。
     */
    public void cacheHotProducts() {
        Page<Product> hotPage = new Page<>(1, 50);
        productMapper.selectPage(hotPage,
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, 1)
                        .orderByDesc(Product::getSales)
        );
        List<Product> hotProducts = hotPage.getRecords();
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
