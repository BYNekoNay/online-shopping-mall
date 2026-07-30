package com.pzhu.mall.modules.recommend.service;

import com.pzhu.mall.modules.behavior.entity.UserBehavior;
import com.pzhu.mall.modules.behavior.mapper.UserBehaviorMapper;
import com.pzhu.mall.modules.product.mapper.ProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 6.3 RecommendCalculateService 单元测试。
 * <p>用固定评分矩阵手工推导余弦相似度，验证 UserCF/ItemCF 数值正确性
 * （C-01 回归：共同商品循环内重复累加内积会导致相似度膨胀 N 倍），
 * 并验证相似商品接口不返回商品自身（C-02 回归）。</p>
 */
class RecommendCalculateServiceTest {

    private UserBehaviorMapper userBehaviorMapper;
    private RecommendCalculateService service;

    @BeforeEach
    void setUp() {
        userBehaviorMapper = mock(UserBehaviorMapper.class);
        service = new RecommendCalculateService();
        inject(service, "userBehaviorMapper", userBehaviorMapper);
        inject(service, "productMapper", mock(ProductMapper.class));
        inject(service, "recommendResultService", mock(RecommendResultService.class));
    }

    // ==================== UserCF（C-01 回归） ====================

    @Test
    void computeUserSimilarity_fixedMatrix_matchesManualCosine() throws Exception {
        // 固定矩阵（已归一化后的评分）：
        // u1: {p1:1.0, p2:0.5}  |u1| = sqrt(1.25)
        // u2: {p1:0.6, p2:0.8}  |u2| = 1.0
        // u3: {p1:1.0, p3:1.0}  |u3| = sqrt(2)
        Map<Long, Map<Long, Double>> matrix = new HashMap<>();
        matrix.put(1L, ratings(Map.of(101L, 1.0, 102L, 0.5)));
        matrix.put(2L, ratings(Map.of(101L, 0.6, 102L, 0.8)));
        matrix.put(3L, ratings(Map.of(101L, 1.0, 103L, 1.0)));

        Map<Long, Map<Long, Double>> sim = invokeComputeUserSimilarity(matrix);

        // 手工推导：
        // dot(u1,u2) = 1*0.6 + 0.5*0.8 = 1.0 → sim = 1.0 / (sqrt(1.25)*1.0) ≈ 0.894427
        // dot(u1,u3) = 1.0              → sim = 1.0 / (sqrt(1.25)*sqrt(2)) ≈ 0.632456
        // dot(u2,u3) = 0.6              → sim = 0.6 / (1.0*sqrt(2))       ≈ 0.424264
        assertEquals(0.894427191, sim.get(1L).get(2L), 1e-9);
        assertEquals(0.632455532, sim.get(1L).get(3L), 1e-9);
        assertEquals(0.424264069, sim.get(2L).get(3L), 1e-9);
        // 对称性
        assertEquals(sim.get(1L).get(2L), sim.get(2L).get(1L), 1e-12);
        // C-01 回归：相似度必须 <= 1（旧 bug 会因内积重复累加而膨胀，甚至 > 1）
        for (Map<Long, Double> row : sim.values()) {
            for (Double v : row.values()) {
                assertTrue(v <= 1.0 + 1e-9, "余弦相似度不应超过 1，实际=" + v);
            }
        }
    }

    @Test
    void computeUserSimilarity_disjointUsers_absent() throws Exception {
        Map<Long, Map<Long, Double>> matrix = new HashMap<>();
        matrix.put(1L, ratings(Map.of(101L, 1.0)));
        matrix.put(2L, ratings(Map.of(202L, 1.0)));

        Map<Long, Map<Long, Double>> sim = invokeComputeUserSimilarity(matrix);
        assertTrue(sim.getOrDefault(1L, Map.of()).isEmpty());
    }

    // ==================== ItemCF ====================

    @Test
    void computeItemSimilarity_fixedMatrix_matchesManualCosine() throws Exception {
        // 用户-商品矩阵：
        // u1: {p1:1.0, p2:0.5}
        // u2: {p1:0.6, p2:0.8, p3:1.0}
        // 商品向量：p1:{u1:1.0,u2:0.6} p2:{u1:0.5,u2:0.8} p3:{u2:1.0}
        Map<Long, Map<Long, Double>> matrix = new HashMap<>();
        matrix.put(1L, ratings(Map.of(101L, 1.0, 102L, 0.5)));
        matrix.put(2L, ratings(Map.of(101L, 0.6, 102L, 0.8, 103L, 1.0)));

        Map<Long, Map<Long, Double>> sim = invokeComputeItemSimilarity(matrix);

        // 手工推导：
        // |p1| = sqrt(1.36), |p2| = sqrt(0.89), |p3| = 1
        // dot(p1,p2) = 1*0.5 + 0.6*0.8 = 0.98 → sim = 0.98/(sqrt(1.36)*sqrt(0.89)) ≈ 0.890762
        // dot(p1,p3) = 0.6 → sim ≈ 0.6/sqrt(1.36) ≈ 0.514496
        // dot(p2,p3) = 0.8 → sim ≈ 0.8/sqrt(0.89) ≈ 0.847998
        assertEquals(0.890762, sim.get(101L).get(102L), 1e-6);
        assertEquals(0.514496, sim.get(101L).get(103L), 1e-6);
        assertEquals(0.847998, sim.get(102L).get(103L), 1e-6);
        assertEquals(sim.get(101L).get(102L), sim.get(102L).get(101L), 1e-12);
    }

    // ==================== 评分矩阵构建 ====================

    @Test
    void buildRatingMatrix_weightsAndDecayAndNormalization() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        List<UserBehavior> behaviors = new ArrayList<>();
        behaviors.add(behavior(1L, 101L, 3, now));                    // 购买 权重5.0 × 衰减≈1.0
        behaviors.add(behavior(1L, 102L, 1, now));                    // 浏览 权重1.0
        behaviors.add(behavior(1L, 103L, 1, now.minusDays(14)));      // 14天前浏览 → exp(-0.7)
        behaviors.add(behavior(1L, 104L, 2, now));                    // 收藏 权重3.0
        behaviors.add(behavior(null, 105L, 1, now));                  // 脏数据：userId 为空 → 跳过
        behaviors.add(behavior(1L, null, 1, now));                    // 脏数据：productId 为空 → 跳过

        Map<Long, Map<Long, Double>> matrix = invokeBuildRatingMatrix(behaviors);

        Map<Long, Double> u1 = matrix.get(1L);
        assertNotNull(u1);
        assertEquals(4, u1.size());
        // 归一化：最大值(购买≈5.0) → 1.0
        assertEquals(1.0, u1.get(101L), 1e-6);
        // 浏览/购买 = 1/5
        assertEquals(0.2, u1.get(102L), 1e-6);
        // 收藏/购买 = 3/5
        assertEquals(0.6, u1.get(104L), 1e-6);
        // 14 天前浏览：exp(-0.05*14)/5 ≈ 0.496585/5 ≈ 0.099317
        assertEquals(0.099317, u1.get(103L), 1e-4);
        // 脏数据不进入矩阵
        assertFalse(u1.containsKey(105L));
        assertNull(matrix.get(null));
    }

    // ==================== normalizeScores ====================

    @Test
    void normalizeScores_minMax() throws Exception {
        Map<Long, Double> in = ratings(Map.of(1L, 2.0, 2L, 4.0, 3L, 3.0));
        Map<Long, Double> out = invokeNormalizeScores(in);
        assertEquals(0.0, out.get(1L), 1e-12);
        assertEquals(0.5, out.get(3L), 1e-12);
        assertEquals(1.0, out.get(2L), 1e-12);
    }

    @Test
    void normalizeScores_allEqual_returnsHalf() throws Exception {
        Map<Long, Double> out = invokeNormalizeScores(ratings(Map.of(1L, 3.0, 2L, 3.0)));
        assertEquals(0.5, out.get(1L), 1e-12);
        assertEquals(0.5, out.get(2L), 1e-12);
    }

    @Test
    void normalizeScores_empty_returnsEmpty() throws Exception {
        assertTrue(invokeNormalizeScores(new HashMap<>()).isEmpty());
    }

    // ==================== computeSimilarProducts（C-02 回归） ====================

    @Test
    void computeSimilarProducts_excludesSelfSortedDescending() {
        // 行为（全部浏览、当前时间 → 归一化后均为 1.0）：
        // u1:{p1,p2} u2:{p1,p2,p3} u3:{p1,p4}
        List<UserBehavior> behaviors = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        behaviors.add(behavior(1L, 101L, 1, now));
        behaviors.add(behavior(1L, 102L, 1, now));
        behaviors.add(behavior(2L, 101L, 1, now));
        behaviors.add(behavior(2L, 102L, 1, now));
        behaviors.add(behavior(2L, 103L, 1, now));
        behaviors.add(behavior(3L, 101L, 1, now));
        behaviors.add(behavior(3L, 104L, 1, now));
        when(userBehaviorMapper.selectList(any())).thenReturn(behaviors);

        Map<Long, Double> result = service.computeSimilarProducts(101L, 10);

        // C-02 回归：结果不得包含目标商品自身
        assertFalse(result.containsKey(101L), "相似商品列表不应包含目标商品自身");
        assertEquals(3, result.size());
        // 手工推导：p1 向量 {u1:1,u2:1,u3:1}, |p1|=sqrt(3)
        // p2: dot=2, |p2|=sqrt(2) → 2/sqrt(6) ≈ 0.816497
        // p3: dot=1, |p3|=1       → 1/sqrt(3) ≈ 0.577350
        // p4: 同 p3
        assertEquals(0.816497, result.get(102L), 1e-6);
        assertEquals(0.577350, result.get(103L), 1e-6);
        assertEquals(0.577350, result.get(104L), 1e-6);
        // 降序排列：p2 排第一
        assertEquals(102L, result.keySet().iterator().next());
    }

    @Test
    void computeSimilarProducts_noBehavior_returnsEmpty() {
        when(userBehaviorMapper.selectList(any())).thenReturn(new ArrayList<>());
        assertTrue(service.computeSimilarProducts(101L, 10).isEmpty());
    }

    @Test
    void computeSimilarProducts_topNLimits() {
        List<UserBehavior> behaviors = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        behaviors.add(behavior(1L, 101L, 1, now));
        behaviors.add(behavior(1L, 102L, 1, now));
        behaviors.add(behavior(2L, 101L, 1, now));
        behaviors.add(behavior(2L, 103L, 1, now));
        behaviors.add(behavior(3L, 101L, 1, now));
        behaviors.add(behavior(3L, 104L, 1, now));
        when(userBehaviorMapper.selectList(any())).thenReturn(behaviors);

        Map<Long, Double> result = service.computeSimilarProducts(101L, 2);
        assertEquals(2, result.size());
    }

    // ==================== helpers ====================

    private static UserBehavior behavior(Long userId, Long productId, int type, LocalDateTime time) {
        UserBehavior b = new UserBehavior();
        b.setUserId(userId);
        b.setProductId(productId);
        b.setBehaviorType(type);
        b.setCreateTime(time);
        return b;
    }

    private static Map<Long, Double> ratings(Map<Long, Double> src) {
        return new HashMap<>(src);
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Map<Long, Double>> invokeComputeUserSimilarity(Map<Long, Map<Long, Double>> matrix)
            throws Exception {
        Method m = RecommendCalculateService.class.getDeclaredMethod("computeUserSimilarity", Map.class);
        m.setAccessible(true);
        return (Map<Long, Map<Long, Double>>) m.invoke(service, matrix);
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Map<Long, Double>> invokeComputeItemSimilarity(Map<Long, Map<Long, Double>> matrix)
            throws Exception {
        Method m = RecommendCalculateService.class.getDeclaredMethod("computeItemSimilarity", Map.class);
        m.setAccessible(true);
        return (Map<Long, Map<Long, Double>>) m.invoke(service, matrix);
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Map<Long, Double>> invokeBuildRatingMatrix(List<UserBehavior> behaviors)
            throws Exception {
        Method m = RecommendCalculateService.class.getDeclaredMethod("buildRatingMatrix", List.class);
        m.setAccessible(true);
        return (Map<Long, Map<Long, Double>>) m.invoke(service, behaviors);
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Double> invokeNormalizeScores(Map<Long, Double> scores) throws Exception {
        Method m = RecommendCalculateService.class.getDeclaredMethod("normalizeScores", Map.class);
        m.setAccessible(true);
        return (Map<Long, Double>) m.invoke(service, scores);
    }

    private static void inject(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
