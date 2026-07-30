package com.pzhu.mall.modules.recommend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pzhu.mall.modules.behavior.entity.UserBehavior;
import com.pzhu.mall.modules.behavior.mapper.UserBehaviorMapper;
import com.pzhu.mall.modules.product.entity.Product;
import com.pzhu.mall.modules.product.mapper.ProductMapper;
import com.pzhu.mall.modules.recommend.entity.RecommendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 推荐结果离线计算服务（UserCF + ItemCF 混合协同过滤）。
 *
 * <p>算法流程：
 * <ol>
 *   <li>从 user_behavior 加载全量行为数据，构建用户-商品评分矩阵（含时间衰减）</li>
 *   <li>分别计算 UserCF 用户相似度矩阵与 ItemCF 商品相似度矩阵（倒排索引优化）</li>
 *   <li>对每个活跃用户，按混合策略（动态 α）融合 UserCF/ItemCF 得分，取 Top-N</li>
 *   <li>冷启动用户：ItemCF 结果 + 热门兜底补位</li>
 *   <li>写入 recommend_result 表，供 RecommendService 查询</li>
 * </ol>
 *
 * @see <a href="12-核心算法设计文档.md">核心算法设计文档</a> §3~§9
 */
@Service
public class RecommendCalculateService {

    private static final Logger log = LoggerFactory.getLogger(RecommendCalculateService.class);

    /** UserCF 近邻数 K */
    private static final int USERCF_K = 20;

    /** ItemCF 相似商品数 M */
    private static final int ITEMCF_M = 10;

    /** 活跃用户阈值（高行为数） */
    private static final int T_HIGH = 20;

    /** 活跃用户阈值（低行为数，低于此值走冷启动） */
    private static final int T_LOW = 5;

    /** 混合权重：高活跃用户的 UserCF 权重 */
    private static final double ALPHA_HIGH = 0.6;

    /** 混合权重：中等活跃用户的 UserCF 权重 */
    private static final double ALPHA_MID = 0.4;

    /** 时间衰减系数 λ（0.05 ≈ 14天衰减至50%） */
    private static final double DECAY_LAMBDA = 0.05;

    /** 热门商品权重：销量 */
    private static final double HOT_W1 = 0.7;

    /** 热门商品权重：浏览量（交互次数） */
    private static final double HOT_W2 = 0.3;

    /** 推荐结果条数 */
    private static final int RECOMMEND_NUM = 10;

    /** 算法类型：混合推荐（UserCF + ItemCF） */
    private static final int ALGO_HYBRID = 3;

    /** 算法类型：热门兜底 */
    private static final int ALGO_HOT = 4;

    @Resource
    private UserBehaviorMapper userBehaviorMapper;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private RecommendResultService recommendResultService;

    /**
     * 全量重算所有活跃用户的推荐结果。
     */
    @Transactional(rollbackFor = Exception.class)
    public void calculateForAll() {
        long start = System.currentTimeMillis();
        log.info("[推荐-全量计算] 开始全量推荐计算...");

        // 1. 分页加载全量行为数据（避免 OOM）
        List<UserBehavior> allBehaviors = new ArrayList<>();
        int pageNum = 1;
        int pageSize = 1000;
        List<UserBehavior> pageData;
        long maxRows = 500_000L; // R3: 50万条以上记录日志告警
        do {
            Page<UserBehavior> page = new Page<>(pageNum++, pageSize);
            pageData = userBehaviorMapper.selectPage(page,
                    new LambdaQueryWrapper<UserBehavior>()
                            .in(UserBehavior::getBehaviorType, 1, 2, 3, 4)
                            .orderByAsc(UserBehavior::getUserId)
            ).getRecords();
            allBehaviors.addAll(pageData);
        } while (pageData.size() == pageSize);
        log.info("[推荐-全量计算] 加载行为数据 {} 条（分页加载）", allBehaviors.size());
        if (allBehaviors.size() > maxRows) {
            log.warn("[推荐-全量计算] 行为数据量 {} 超过阈值 {}，可能导致 OOM 或长时间计算", allBehaviors.size(), maxRows);
        }

        // 2. 构建评分矩阵（稀疏存储：userId -> (productId -> score)）
        Map<Long, Map<Long, Double>> ratingMatrix = buildRatingMatrix(allBehaviors);
        int activeUserCount = ratingMatrix.keySet().size();
        log.info("[推荐-全量计算] 构建评分矩阵完成，活跃用户数={}", activeUserCount);

        // 3. 加载全部商品映射（仅保留上架商品）
        List<Product> allProducts = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, 1)
                        .eq(Product::getIsDeleted, 0)
        );
        Map<Long, Product> productMap = allProducts.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        log.info("[推荐-全量计算] 加载上架商品 {} 个", allProducts.size());

        // 4. 计算 UserCF 用户相似度
        long t1 = System.currentTimeMillis();
        Map<Long, Map<Long, Double>> userSimilarity = computeUserSimilarity(ratingMatrix);
        log.info("[推荐-全量计算] UserCF 相似度计算完成，耗时={}ms，有效用户对数={}",
                System.currentTimeMillis() - t1, userSimilarity.size());

        // 5. 计算 ItemCF 商品相似度
        t1 = System.currentTimeMillis();
        Map<Long, Map<Long, Double>> itemSimilarity = computeItemSimilarity(ratingMatrix);
        log.info("[推荐-全量计算] ItemCF 相似度计算完成，耗时={}ms，有效商品对数={}",
                System.currentTimeMillis() - t1, itemSimilarity.size());

        // 6. 计算热门商品排序（冷启动兜底）
        t1 = System.currentTimeMillis();
        List<Map.Entry<Long, Double>> hotRank = computeHotRank(allProducts, ratingMatrix);
        log.info("[推荐-全量计算] 热门排序计算完成，耗时={}ms，TOP3: {}",
                System.currentTimeMillis() - t1,
                hotRank.stream().limit(3).map(e -> e.getKey() + "=" + String.format("%.4f", e.getValue())).toList());

        // 7. 对每个有行为的用户生成推荐
        LocalDateTime now = LocalDateTime.now();
        List<RecommendResult> allResults = new ArrayList<>();
        int hybridCount = 0, coldStartCount = 0, hotFallbackCount = 0;

        for (Long userId : ratingMatrix.keySet()) {
            Map<Long, Double> userRatings = ratingMatrix.get(userId);
            int behaviorCount = userRatings.size();

            List<RecommendResult> userResults;
            if (behaviorCount == 0) {
                // 纯新用户：纯热门兜底（user_id=NULL，全局共享）
                userResults = buildHotResults(null, hotRank, productMap, ALGO_HOT);
                hotFallbackCount++;
            } else if (behaviorCount < T_LOW) {
                // 低活跃：ItemCF + 热门补位
                userResults = buildColdStartResults(userId, userRatings, itemSimilarity, hotRank, productMap);
                coldStartCount++;
            } else {
                // 正常用户：UserCF + ItemCF 混合
                userResults = buildHybridResults(userId, userRatings, ratingMatrix, userSimilarity, itemSimilarity, productMap);
                hybridCount++;
            }

            for (RecommendResult r : userResults) {
                r.setGenerateTime(now);
            }
            allResults.addAll(userResults);
        }

        // 8. 为纯新用户（无任何行为）也生成全局热门兜底
        List<Long> allUserIds = userBehaviorMapper.selectList(
                new LambdaQueryWrapper<UserBehavior>().select(UserBehavior::getUserId)
        ).stream().map(UserBehavior::getUserId).collect(Collectors.toList());
        Set<Long> existingUserIds = new HashSet<>(ratingMatrix.keySet());
        // 注意：纯新用户（无 user_behavior 记录）不在 ratingMatrix 中
        // 他们的推荐结果将在查询时动态返回热门兜底，无需在此预生成

        // 9. 批量写入数据库（清旧数据 + batch insert，事务保护）
        if (!allResults.isEmpty()) {
            boolean removed = recommendResultService.remove(new LambdaQueryWrapper<>());
            log.info("[推荐-全量计算] 清除旧推荐结果 {}", removed);

            recommendResultService.saveBatch(allResults, 500);
            log.info("[推荐-全量计算] 写入新推荐结果 {} 条", allResults.size());
        }

        long totalMs = System.currentTimeMillis() - start;
        log.info("[推荐-全量计算] 完成！混合={} 冷启动={} 热门兜底={} 总耗时={}ms",
                hybridCount, coldStartCount, hotFallbackCount, totalMs);
    }

    // ==================== 评分矩阵构建 ====================

    /**
     * 从行为记录构建用户-商品评分矩阵。
     *
     * <p>评分 = 权重 × 时间衰减，同一用户-商品对的多条行为累加。
     * <p>仅纳入 behavior_type ∈ {1,2,3,4} 的行为（排除系统生成的无效记录）。
     */
    private Map<Long, Map<Long, Double>> buildRatingMatrix(List<UserBehavior> behaviors) {
        Map<Long, Map<Long, Double>> matrix = new HashMap<>();

        for (UserBehavior b : behaviors) {
            if (b.getUserId() == null || b.getProductId() == null || b.getBehaviorType() == null) {
                continue;
            }

            double weight = getWeight(b.getBehaviorType());
            double decay = computeDecay(b.getCreateTime());
            double score = weight * decay;

            matrix.computeIfAbsent(b.getUserId(), k -> new HashMap<>())
                    .merge(b.getProductId(), score, Double::sum);
        }

        // min-max 归一化（每个用户独立归一化，避免活跃用户分数普遍偏高）
        for (Map<Long, Double> userRatings : matrix.values()) {
            normalize(userRatings);
        }

        return matrix;
    }

    /**
     * 行为类型 → 权重映射。
     *
     * <p>与 {@code 12-核心算法设计文档.md §3.1} 一致：
     * 浏览=1.0, 收藏=3.0, 购买=5.0, 评价(好评)=4.0
     */
    private double getWeight(Integer behaviorType) {
        return switch (behaviorType) {
            case 2 -> 3.0;   // 收藏
            case 3 -> 5.0;   // 购买
            case 4 -> 4.0;   // 评价（好评）
            default -> 1.0;  // 浏览
        };
    }

    /**
     * 时间衰减函数。
     *
     * <p>decay(t) = exp(-λ × days)，λ=0.05 对应约14天衰减至50%。
     */
    private double computeDecay(LocalDateTime createTime) {
        if (createTime == null) {
            return 1.0;
        }
        double days = java.time.Duration.between(createTime, LocalDateTime.now()).toHours() / 24.0;
        return Math.exp(-DECAY_LAMBDA * days);
    }

    /**
     * 对单个用户的评分做 min-max 归一化到 [0, 1]。
     */
    private void normalize(Map<Long, Double> ratings) {
        double max = ratings.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        if (max > 0) {
            for (Map.Entry<Long, Double> e : ratings.entrySet()) {
                e.setValue(e.getValue() / max);
            }
        }
    }

    // ==================== UserCF ====================

    /**
     * 计算用户相似度矩阵（倒排索引优化）。
     *
     * <p>复杂度：O(Σ|item_users_i|²)，远优于 O(m²n)。
     */
    private Map<Long, Map<Long, Double>> computeUserSimilarity(Map<Long, Map<Long, Double>> ratingMatrix) {
        // 构建"商品→交互用户列表"倒排索引
        Map<Long, List<Long>> itemUsers = new HashMap<>();
        for (Map.Entry<Long, Map<Long, Double>> entry : ratingMatrix.entrySet()) {
            Long userId = entry.getKey();
            for (Long productId : entry.getValue().keySet()) {
                itemUsers.computeIfAbsent(productId, k -> new ArrayList<>()).add(userId);
            }
        }

        // 计算每个用户评分向量的模长
        Map<Long, Double> userNorm = new HashMap<>();
        for (Map.Entry<Long, Map<Long, Double>> entry : ratingMatrix.entrySet()) {
            double norm = Math.sqrt(entry.getValue().values().stream()
                    .mapToDouble(v -> v * v).sum());
            userNorm.put(entry.getKey(), norm > 0 ? norm : 1.0);
        }

        // 仅对有共同商品的用户对累加内积（逐商品累加 r[u][item] * r[v][item]）
        Map<Long, Map<Long, Double>> coRate = new HashMap<>();
        for (Map.Entry<Long, List<Long>> itemEntry : itemUsers.entrySet()) {
            Long itemId = itemEntry.getKey();
            List<Long> users = itemEntry.getValue();
            for (int i = 0; i < users.size(); i++) {
                for (int j = i + 1; j < users.size(); j++) {
                    Long u = users.get(i);
                    Long v = users.get(j);
                    double ri = ratingMatrix.get(u).getOrDefault(itemId, 0.0);
                    double rj = ratingMatrix.get(v).getOrDefault(itemId, 0.0);
                    double productScore = ri * rj;
                    if (productScore > 0) {
                        coRate.computeIfAbsent(u, k -> new HashMap<>()).merge(v, productScore, Double::sum);
                        coRate.computeIfAbsent(v, k -> new HashMap<>()).merge(u, productScore, Double::sum);
                    }
                }
            }
        }

        // 余弦相似度
        Map<Long, Map<Long, Double>> similarity = new HashMap<>();
        for (Map.Entry<Long, Map<Long, Double>> entry : coRate.entrySet()) {
            Long u = entry.getKey();
            Map<Long, Double> simMap = new HashMap<>();
            for (Map.Entry<Long, Double> e : entry.getValue().entrySet()) {
                Long v = e.getKey();
                double sim = e.getValue() / (userNorm.get(u) * userNorm.get(v));
                simMap.put(v, sim);
            }
            similarity.put(u, simMap);
        }

        return similarity;
    }

    // ==================== ItemCF ====================

    /**
     * 计算商品相似度矩阵（倒排索引优化）。
     *
     * <p>复杂度：O(Σ|H(u)|²)，H(u) 为用户 u 交互过的商品集合。
     */
    private Map<Long, Map<Long, Double>> computeItemSimilarity(Map<Long, Map<Long, Double>> ratingMatrix) {
        // 构建"用户→交互商品列表"倒排索引
        Map<Long, List<Long>> userItems = new HashMap<>();
        for (Map.Entry<Long, Map<Long, Double>> entry : ratingMatrix.entrySet()) {
            userItems.put(entry.getKey(), new ArrayList<>(entry.getValue().keySet()));
        }

        // 计算每个商品评分向量的模长
        Map<Long, Double> itemNorm = new HashMap<>();
        for (Map<Long, Double> userRatings : ratingMatrix.values()) {
            for (Map.Entry<Long, Double> e : userRatings.entrySet()) {
                itemNorm.merge(e.getKey(), e.getValue() * e.getValue(), Double::sum);
            }
        }
        for (Map.Entry<Long, Double> e : itemNorm.entrySet()) {
            itemNorm.put(e.getKey(), Math.sqrt(e.getValue()));
        }

        // 仅对同一用户交互过的商品对累加内积
        Map<Long, Map<Long, Double>> coRate = new HashMap<>();
        for (Map.Entry<Long, List<Long>> entry : userItems.entrySet()) {
            List<Long> items = entry.getValue();
            Map<Long, Double> userRatings = ratingMatrix.get(entry.getKey());
            if (userRatings == null || items.size() < 2) {
                continue;
            }
            for (int i = 0; i < items.size(); i++) {
                for (int j = i + 1; j < items.size(); j++) {
                    Long pi = items.get(i);
                    Long pj = items.get(j);
                    double ri = userRatings.getOrDefault(pi, 0.0);
                    double rj = userRatings.getOrDefault(pj, 0.0);
                    double productScore = ri * rj;
                    if (productScore > 0) {
                        coRate.computeIfAbsent(pi, k -> new HashMap<>()).merge(pj, productScore, Double::sum);
                        coRate.computeIfAbsent(pj, k -> new HashMap<>()).merge(pi, productScore, Double::sum);
                    }
                }
            }
        }

        // 余弦相似度
        Map<Long, Map<Long, Double>> similarity = new HashMap<>();
        for (Map.Entry<Long, Map<Long, Double>> entry : coRate.entrySet()) {
            Long pi = entry.getKey();
            Map<Long, Double> simMap = new HashMap<>();
            for (Map.Entry<Long, Double> e : entry.getValue().entrySet()) {
                Long pj = e.getKey();
                double normI = itemNorm.getOrDefault(pi, 1.0);
                double normJ = itemNorm.getOrDefault(pj, 1.0);
                double denom = normI * normJ;
                if (denom > 0) {
                    double sim = e.getValue() / denom;
                    simMap.put(pj, sim);
                }
            }
            similarity.put(pi, simMap);
        }

        return similarity;
    }

    // ==================== 热门排序 ====================

    /**
     * 计算热门商品排序（用于冷启动兜底）。
     *
     * <p>hot_score(i) = 0.7 × normalized_sales(i) + 0.3 × normalized_interactions(i)
     *
     * <p>使用 product.sales 作为销量指标（DB字段），用评分矩阵中交互用户数近似浏览量。
     * 仅对状态为上架（status=1）的商品计算。
     */
    private List<Map.Entry<Long, Double>> computeHotRank(List<Product> products,
                                                          Map<Long, Map<Long, Double>> ratingMatrix) {
        // 从评分矩阵统计每个商品的交互用户数（近似浏览量）
        Map<Long, Integer> interactionCount = new HashMap<>();
        for (Map<Long, Double> userRatings : ratingMatrix.values()) {
            for (Long pid : userRatings.keySet()) {
                interactionCount.merge(pid, 1, Integer::sum);
            }
        }

        // 计算销量的最大最小值（用于归一化）
        double maxSales = products.stream().mapToDouble(p -> p.getSales() != null ? p.getSales() : 0).max().orElse(0.0);
        double maxSalesSafe = Math.max(maxSales, 1.0);

        // 计算交互数的最大最小值（用于归一化）
        double maxInteract = interactionCount.values().stream().mapToDouble(Integer::doubleValue).max().orElse(0.0);
        double maxInteractSafe = Math.max(maxInteract, 1.0);

        List<Map.Entry<Long, Double>> ranked = new ArrayList<>();
        for (Product p : products) {
            double normSales = (p.getSales() != null ? p.getSales() : 0) / maxSalesSafe;
            double normViews = interactionCount.getOrDefault(p.getId(), 0) / maxInteractSafe;
            double hotScore = HOT_W1 * normSales + HOT_W2 * normViews;
            ranked.add(new AbstractMap.SimpleEntry<>(p.getId(), hotScore));
        }
        ranked.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        return ranked;
    }

    // ==================== 推荐生成 ====================

    /**
     * 高活跃用户：UserCF + ItemCF 混合推荐。
     */
    private List<RecommendResult> buildHybridResults(Long userId,
                                                      Map<Long, Double> userRatings,
                                                      Map<Long, Map<Long, Double>> ratingMatrix,
                                                      Map<Long, Map<Long, Double>> userSimilarity,
                                                      Map<Long, Map<Long, Double>> itemSimilarity,
                                                      Map<Long, Product> productMap) {
        // 1. UserCF 推荐分
        Map<Long, Double> usercfScores = computeUserCFScore(userId, userRatings, ratingMatrix, userSimilarity);

        // 2. ItemCF 推荐分
        Map<Long, Double> itemcfScores = computeItemCFScore(userId, userRatings, itemSimilarity);

        // 3. min-max 归一化
        Map<Long, Double> normUserCF = normalizeScores(usercfScores);
        Map<Long, Double> normItemCF = normalizeScores(itemcfScores);

        // 4. 混合（按行为数动态调整 α）
        double alpha = userRatings.size() >= T_HIGH ? ALPHA_HIGH : ALPHA_MID;
        log.debug("[推荐-混合] 用户={} 行为数={} α={}", userId, userRatings.size(), String.format("%.1f", alpha));

        Set<Long> candidates = new HashSet<>();
        candidates.addAll(normUserCF.keySet());
        candidates.addAll(normItemCF.keySet());

        Map<Long, Double> finalScores = new HashMap<>();
        for (Long pid : candidates) {
            double usercf = normUserCF.getOrDefault(pid, 0.0);
            double itemcf = normItemCF.getOrDefault(pid, 0.0);
            double score = alpha * usercf + (1 - alpha) * itemcf;
            if (score > 0) {
                finalScores.put(pid, score);
            }
        }

        return buildResults(userId, finalScores, productMap, ALGO_HYBRID);
    }

    /**
     * UserCF 推荐分计算。
     */
    private Map<Long, Double> computeUserCFScore(Long targetUserId,
                                                  Map<Long, Double> userRatings,
                                                  Map<Long, Map<Long, Double>> ratingMatrix,
                                                  Map<Long, Map<Long, Double>> userSimilarity) {
        Map<Long, Double> scores = new HashMap<>();
        Map<Long, Double> neighbors = userSimilarity.getOrDefault(targetUserId, Collections.emptyMap());

        // 取 Top-K 近邻
        List<Map.Entry<Long, Double>> topNeighbors = neighbors.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(USERCF_K)
                .toList();

        Set<Long> interacted = userRatings.keySet();
        int neighborCount = 0;

        for (Map.Entry<Long, Double> neighbor : topNeighbors) {
            Long neighborId = neighbor.getKey();
            double sim = neighbor.getValue();
            Map<Long, Double> neighborRatings = ratingMatrix.get(neighborId);
            if (neighborRatings == null) {
                continue;
            }
            neighborCount++;
            for (Map.Entry<Long, Double> e : neighborRatings.entrySet()) {
                Long pid = e.getKey();
                if (interacted.contains(pid)) {
                    continue; // 跳过已交互商品
                }
                scores.merge(pid, sim * e.getValue(), Double::sum);
            }
        }

        log.debug("[推荐-UserCF] 用户={} 有效近邻数={}", targetUserId, neighborCount);
        return scores;
    }

    /**
     * ItemCF 推荐分计算。
     */
    private Map<Long, Double> computeItemCFScore(Long userId,
                                                  Map<Long, Double> userRatings,
                                                  Map<Long, Map<Long, Double>> itemSimilarity) {
        Map<Long, Double> scores = new HashMap<>();

        for (Map.Entry<Long, Double> entry : userRatings.entrySet()) {
            Long productId = entry.getKey();
            double rating = entry.getValue();
            Map<Long, Double> similarItems = itemSimilarity.getOrDefault(productId, Collections.emptyMap());

            List<Map.Entry<Long, Double>> topSimilar = similarItems.entrySet().stream()
                    .filter(e -> e.getValue() > 0)
                    .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                    .limit(ITEMCF_M)
                    .toList();

            for (Map.Entry<Long, Double> simEntry : topSimilar) {
                Long similarId = simEntry.getKey();
                if (userRatings.containsKey(similarId)) {
                    continue; // 跳过已交互
                }
                scores.merge(similarId, simEntry.getValue() * rating, Double::sum);
            }
        }

        return scores;
    }

    /**
     * 冷启动用户：ItemCF + 热门补位。
     *
     * <p>行为数 < T_LOW 时调用，先取 ItemCF 结果，再用热门商品补足到 RECOMMEND_NUM 条。
     */
    private List<RecommendResult> buildColdStartResults(Long userId,
                                                         Map<Long, Double> userRatings,
                                                         Map<Long, Map<Long, Double>> itemSimilarity,
                                                         List<Map.Entry<Long, Double>> hotRank,
                                                         Map<Long, Product> productMap) {
        // ItemCF 部分
        Map<Long, Double> itemcfScores = computeItemCFScore(userId, userRatings, itemSimilarity);
        Map<Long, Double> normItemCF = normalizeScores(itemcfScores);

        // 取 ItemCF 结果
        List<RecommendResult> results = buildResults(userId, normItemCF, productMap, ALGO_HYBRID);

        // 热门补位：取未出现在 ItemCF 结果中的热门商品
        int fillCount = RECOMMEND_NUM - results.size();
        if (fillCount > 0) {
            Set<Long> existingProductIds = results.stream()
                    .map(RecommendResult::getProductId)
                    .collect(Collectors.toSet());

            List<RecommendResult> hotResults = buildHotResults(userId, hotRank, productMap, ALGO_HOT);
            for (RecommendResult hr : hotResults) {
                if (results.size() >= RECOMMEND_NUM) {
                    break;
                }
                if (!existingProductIds.contains(hr.getProductId())) {
                    results.add(hr);
                }
            }
        }

        log.debug("[推荐-冷启动] 用户={} ItemCF={}条 + 热门补位={}条 总计={}条",
                userId, itemcfScores.size(), Math.max(0, RECOMMEND_NUM - results.size()), results.size());

        return results;
    }

    /**
     * 纯热门兜底（新用户无任何行为，或全局热门缓存）。
     *
     * <p>userId=null 时写入全局热门（user_id IS NULL），供未登录用户使用。
     */
    private List<RecommendResult> buildHotResults(Long userId,
                                                   List<Map.Entry<Long, Double>> hotRank,
                                                   Map<Long, Product> productMap,
                                                   int algorithmType) {
        List<RecommendResult> results = new ArrayList<>();
        int count = 0;
        for (Map.Entry<Long, Double> entry : hotRank) {
            if (count >= RECOMMEND_NUM) {
                break;
            }
            Product p = productMap.get(entry.getKey());
            if (p == null || p.getStatus() != 1) {
                continue;
            }
            RecommendResult r = new RecommendResult();
            r.setUserId(userId);
            r.setProductId(p.getId());
            r.setAlgorithmType(algorithmType);
            r.setScore(BigDecimal.valueOf(entry.getValue()).setScale(4, RoundingMode.HALF_UP));
            results.add(r);
            count++;
        }
        return results;
    }

    // ==================== 工具方法 ====================

    /**
     * 将评分映射转换为 RecommendResult 列表（取 Top-N，过滤下架商品）。
     */
    private List<RecommendResult> buildResults(Long userId,
                                                Map<Long, Double> scores,
                                                Map<Long, Product> productMap,
                                                int algorithmType) {
        return scores.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(RECOMMEND_NUM)
                .map(e -> {
                    Product p = productMap.get(e.getKey());
                    if (p == null || p.getStatus() != 1) {
                        return null;
                    }
                    RecommendResult r = new RecommendResult();
                    r.setUserId(userId);
                    r.setProductId(p.getId());
                    r.setAlgorithmType(algorithmType);
                    r.setScore(BigDecimal.valueOf(e.getValue()).setScale(4, RoundingMode.HALF_UP));
                    return r;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * min-max 归一化到 [0, 1]。
     *
     * <p>所有值相等时统一返回 0.5（避免除零）。
     */
    private Map<Long, Double> normalizeScores(Map<Long, Double> scores) {
        Map<Long, Double> result = new HashMap<>();
        if (scores.isEmpty()) {
            return result;
        }
        double min = scores.values().stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        double max = scores.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        double range = max - min;
        if (range == 0) {
            for (Long pid : scores.keySet()) {
                result.put(pid, 0.5);
            }
            return result;
        }
        for (Map.Entry<Long, Double> e : scores.entrySet()) {
            result.put(e.getKey(), (e.getValue() - min) / range);
        }
        return result;
    }

    /**
     * 为指定用户增量计算推荐结果（替换该用户旧记录）。
     *
     * <p>用于"用户购买后准实时刷新"场景（12-核心算法设计文档.md §9.2）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void calculateForUser(Long userId) {
        long start = System.currentTimeMillis();
        log.info("[推荐-单用户计算] 用户={} 开始", userId);

        // 1. 加载该用户行为
        List<UserBehavior> userBehaviors = userBehaviorMapper.selectList(
                new LambdaQueryWrapper<UserBehavior>()
                        .eq(UserBehavior::getUserId, userId)
                        .in(UserBehavior::getBehaviorType, 1, 2, 3, 4)
        );

        Map<Long, Map<Long, Double>> userMatrix = buildRatingMatrix(userBehaviors);
        Map<Long, Double> userRatings = userMatrix.get(userId);
        if (userRatings == null || userRatings.isEmpty()) {
            log.info("[推荐-单用户计算] 用户={} 无行为数据，跳过", userId);
            return;
        }

        // 2. 加载全量数据计算相似度（R3: 加阈值检查，超量记录告警）
        List<UserBehavior> allBehaviors = userBehaviorMapper.selectList(
                new LambdaQueryWrapper<UserBehavior>()
                        .in(UserBehavior::getBehaviorType, 1, 2, 3, 4)
        );
        if (allBehaviors.size() > 100_000) {
            log.warn("[推荐-单用户计算] 全量行为数据 {} 条，单用户增量计算可能性能不足，建议使用全量重算定时任务", allBehaviors.size());
        }
        Map<Long, Map<Long, Double>> fullMatrix = buildRatingMatrix(allBehaviors);

        List<Product> allProducts = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, 1)
                        .eq(Product::getIsDeleted, 0)
        );
        Map<Long, Product> productMap = allProducts.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        Map<Long, Map<Long, Double>> userSim = computeUserSimilarity(fullMatrix);
        Map<Long, Map<Long, Double>> itemSim = computeItemSimilarity(fullMatrix);

        // 3. 根据行为数选择策略
        List<RecommendResult> results;
        if (userRatings.size() < T_LOW) {
            List<Map.Entry<Long, Double>> hotRank = computeHotRank(allProducts, fullMatrix);
            results = buildColdStartResults(userId, userRatings, itemSim, hotRank, productMap);
        } else {
            results = buildHybridResults(userId, userRatings, fullMatrix, userSim, itemSim, productMap);
        }

        // 4. 写入数据库（先删旧数据再批量插入）
        if (!results.isEmpty()) {
            recommendResultService.remove(
                    new LambdaQueryWrapper<RecommendResult>().eq(RecommendResult::getUserId, userId)
            );
            for (RecommendResult r : results) {
                r.setGenerateTime(LocalDateTime.now());
            }
            recommendResultService.saveBatch(results);
            log.info("[推荐-单用户计算] 用户={} 生成{}条推荐，耗时={}ms",
                    userId, results.size(), System.currentTimeMillis() - start);
        } else {
            log.info("[推荐-单用户计算] 用户={} 无推荐结果", userId);
        }
    }

    /**
     * 计算指定商品的 Top-N 相似商品（ItemCF 余弦相似度，实时计算）。
     *
     * <p>用于"相似商品推荐"查询接口。构建商品评分向量（用户维度），
     * 计算目标商品与其他商品的余弦相似度，按相似度降序返回 Top-N。
     *
     * @param productId 目标商品 ID
     * @param topN      返回数量上限
     * @return 有序映射：商品ID → 相似度（0~1），按相似度降序；目标商品无行为数据时返回空
     */
    public Map<Long, Double> computeSimilarProducts(Long productId, int topN) {
        // 1. 加载全量行为，构建用户-商品评分矩阵
        List<UserBehavior> allBehaviors = userBehaviorMapper.selectList(
                new LambdaQueryWrapper<UserBehavior>()
                        .in(UserBehavior::getBehaviorType, 1, 2, 3, 4)
        );
        Map<Long, Map<Long, Double>> ratingMatrix = buildRatingMatrix(allBehaviors);

        // 2. 转置为商品向量：productId -> (userId -> score)
        Map<Long, Map<Long, Double>> itemVectors = new HashMap<>();
        for (Map.Entry<Long, Map<Long, Double>> userEntry : ratingMatrix.entrySet()) {
            Long userId = userEntry.getKey();
            for (Map.Entry<Long, Double> e : userEntry.getValue().entrySet()) {
                itemVectors.computeIfAbsent(e.getKey(), k -> new HashMap<>()).put(userId, e.getValue());
            }
        }

        Map<Long, Double> targetVector = itemVectors.get(productId);
        if (targetVector == null || targetVector.isEmpty()) {
            log.info("[推荐-相似商品] 商品={} 无行为数据，无法计算相似度", productId);
            return Collections.emptyMap();
        }
        double targetNorm = Math.sqrt(targetVector.values().stream().mapToDouble(v -> v * v).sum());
        if (targetNorm <= 0) {
            return Collections.emptyMap();
        }

        // 3. 逐一计算余弦相似度（仅统计与目标商品有共同用户的商品）
        Map<Long, Double> scores = new HashMap<>();
        for (Map.Entry<Long, Map<Long, Double>> itemEntry : itemVectors.entrySet()) {
            Long otherId = itemEntry.getKey();
            if (otherId.equals(productId)) {
                continue;
            }
            Map<Long, Double> otherVector = itemEntry.getValue();
            double dot = 0.0;
            for (Map.Entry<Long, Double> e : targetVector.entrySet()) {
                Double otherRating = otherVector.get(e.getKey());
                if (otherRating != null) {
                    dot += e.getValue() * otherRating;
                }
            }
            if (dot > 0) {
                double otherNorm = Math.sqrt(otherVector.values().stream().mapToDouble(v -> v * v).sum());
                if (otherNorm > 0) {
                    scores.put(otherId, dot / (targetNorm * otherNorm));
                }
            }
        }

        // 4. 取 Top-N，按相似度降序
        Map<Long, Double> topResults = new LinkedHashMap<>();
        scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(topN)
                .forEach(e -> topResults.put(e.getKey(), e.getValue()));
        log.info("[推荐-相似商品] 商品={} 计算出{}个相似商品", productId, topResults.size());
        return topResults;
    }
}
