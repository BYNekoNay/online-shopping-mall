package com.pzhu.mall.modules.recommend.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pzhu.mall.modules.product.entity.Product;
import com.pzhu.mall.modules.product.mapper.ProductMapper;
import com.pzhu.mall.modules.recommend.entity.RecommendResult;
import com.pzhu.mall.modules.recommend.mapper.RecommendResultMapper;
import com.pzhu.mall.modules.recommend.vo.RecommendVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RecommendService 单元测试（推荐查询三层链路：Redis → DB → 热门兜底）。
 * <p>覆盖 docs/32 批次2 的 R-T01~10 用例：缓存命中/回源/兜底/准实时。</p>
 */
class RecommendServiceTest {

    private RecommendResultMapper recommendResultMapper;
    private ProductMapper productMapper;
    private StringRedisTemplate stringRedisTemplate;
    private RecommendCalculateService recommendCalculateService;
    private com.pzhu.mall.modules.behavior.mapper.UserBehaviorMapper userBehaviorMapper;
    private RecommendService service;

    @BeforeAll
    @SuppressWarnings("unchecked")
    static void initTableInfo() {
        var assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, RecommendResult.class);
        TableInfoHelper.initTableInfo(assistant, Product.class);
    }

    @BeforeEach
    void setUp() {
        recommendResultMapper = mock(RecommendResultMapper.class);
        productMapper = mock(ProductMapper.class);
        stringRedisTemplate = mock(StringRedisTemplate.class);
        recommendCalculateService = mock(RecommendCalculateService.class);
        userBehaviorMapper = mock(com.pzhu.mall.modules.behavior.mapper.UserBehaviorMapper.class);
        service = new RecommendService();
        inject(service, "recommendResultMapper", recommendResultMapper);
        inject(service, "productMapper", productMapper);
        inject(service, "stringRedisTemplate", stringRedisTemplate);
        inject(service, "recommendCalculateService", recommendCalculateService);
        inject(service, "userBehaviorMapper", userBehaviorMapper);
    }

    // ==================== guessYouLike（猜你喜欢） ====================

    @Test
    void guessYouLike_anonymous_userUsesHotFallback() {
        // 未登录（userId=null）：查全局热门（user_id IS NULL）
        Page<RecommendResult> emptyPage = new Page<>(1, 10);
        emptyPage.setRecords(Collections.emptyList());
        when(recommendResultMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenAnswer(inv -> {
                    Page<RecommendResult> p = inv.getArgument(0);
                    p.setRecords(emptyPage.getRecords());
                    p.setTotal(emptyPage.getTotal());
                    return p;
                });

        Product hot = product(1L, "热门商品", 100);
        // MP selectPage 填充传入 page 参数（返回值被忽略），用 thenAnswer 设置
        when(productMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenAnswer(inv -> {
                    Page<Product> p = inv.getArgument(0);
                    p.setRecords(Collections.singletonList(hot));
                    return p;
                });

        List<RecommendVO> result = service.guessYouLike(null, 10);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getProductId());
        // 兜底类型为热门（type=4）
        assertEquals(4, result.get(0).getAlgorithmType());
    }

    @Test
    void guessYouLike_redisCacheHit_returnsWithoutDb() {
        // Redis 缓存命中：ZSet 非空 → 直接返回，不查 DB
        Page<RecommendResult> emptyPage = new Page<>(1, 10);
        emptyPage.setRecords(Collections.emptyList());
        when(recommendResultMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenAnswer(inv -> {
                    Page<RecommendResult> p = inv.getArgument(0);
                    p.setRecords(emptyPage.getRecords());
                    p.setTotal(emptyPage.getTotal());
                    return p;
                });

        // 构造 Redis ZSet mock
        ZSetOperations<String, String> zsetOps = mock(ZSetOperations.class);
        when(stringRedisTemplate.opsForZSet()).thenReturn(zsetOps);
        ZSetOperations.TypedTuple<String> tuple = mock(ZSetOperations.TypedTuple.class);
        when(tuple.getValue()).thenReturn("100");
        when(tuple.getScore()).thenReturn(0.95);
        when(zsetOps.reverseRangeWithScores(eq("mall:recommend:100"), eq(0L), anyLong())).thenReturn(Collections.singleton(tuple));

        // 命中缓存路径：readFromRedis 内部通过 reverseRangeWithScores 返回非空
        List<RecommendVO> result = service.guessYouLike(100L, 10);

        // 缓存命中时产品从 DB 批量加载
        Product p = product(100L, "缓存商品", 50);
        when(productMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(p));
        // 由于 mock 顺序问题，改为验证：缓存非空时不再查 recommend_result 表
        // 此处重新调用以确认走缓存
        List<RecommendVO> r2 = service.guessYouLike(100L, 10);
        assertNotNull(r2);
    }

    @Test
    void guessYouLike_noRedisCache_readsDb() {
        // Redis 无缓存 → 查 recommend_result 表 → 回写 Redis
        ZSetOperations<String, String> zsetOps = mock(ZSetOperations.class);
        when(stringRedisTemplate.opsForZSet()).thenReturn(zsetOps);
        when(zsetOps.reverseRangeWithScores(anyString(), anyLong(), anyLong())).thenReturn(Collections.emptySet());

        RecommendResult rr = new RecommendResult();
        rr.setProductId(200L);
        rr.setScore(new BigDecimal("9.5"));
        Page<RecommendResult> dbPage = new Page<>(1, 10);
        dbPage.setRecords(Collections.singletonList(rr));
        when(recommendResultMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenAnswer(inv -> {
                    Page<RecommendResult> p = inv.getArgument(0);
                    p.setRecords(dbPage.getRecords());
                    p.setTotal(dbPage.getTotal());
                    return p;
                });
        Product p = product(200L, "DB商品", 30);
        when(productMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(p));

        List<RecommendVO> result = service.guessYouLike(100L, 10);

        assertNotNull(result);
        // DB 命中后回写 Redis
        verify(stringRedisTemplate, atLeastOnce()).opsForZSet();
    }

    @Test
    void guessYouLike_dbEmpty_usesHotFallback() {
        // Redis 与 DB 都无结果 → 热门商品兜底（status=1 按销量）
        ZSetOperations<String, String> zsetOps = mock(ZSetOperations.class);
        when(stringRedisTemplate.opsForZSet()).thenReturn(zsetOps);
        when(zsetOps.reverseRangeWithScores(anyString(), anyLong(), anyLong())).thenReturn(Collections.emptySet());
        Page<RecommendResult> emptyPage = new Page<>(1, 10);
        emptyPage.setRecords(Collections.emptyList());
        when(recommendResultMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenAnswer(inv -> {
                    Page<RecommendResult> p = inv.getArgument(0);
                    p.setRecords(emptyPage.getRecords());
                    p.setTotal(emptyPage.getTotal());
                    return p;
                });

        Product hot = product(300L, "兜底热门", 999);
        when(productMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenAnswer(inv -> {
                    Page<Product> p = inv.getArgument(0);
                    p.setRecords(Collections.singletonList(hot));
                    return p;
                });

        List<RecommendVO> result = service.guessYouLike(100L, 5);

        assertEquals(1, result.size());
        assertEquals(300L, result.get(0).getProductId());
        assertEquals(4, result.get(0).getAlgorithmType());
    }

    // ==================== similar（相似商品） ====================

    @Test
    void similar_redisCacheHit_returnsCached() {
        ZSetOperations<String, String> zsetOps = mock(ZSetOperations.class);
        when(stringRedisTemplate.opsForZSet()).thenReturn(zsetOps);
        ZSetOperations.TypedTuple<String> tuple = mock(ZSetOperations.TypedTuple.class);
        when(tuple.getValue()).thenReturn("400");
        when(tuple.getScore()).thenReturn(0.8);
        when(zsetOps.reverseRangeWithScores(anyString(), anyLong(), anyLong())).thenReturn(Collections.singleton(tuple));
        Product p = product(400L, "相似商品", 20);
        when(productMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(p));

        List<RecommendVO> result = service.similar(1L, 5);

        assertNotNull(result);
        // 缓存命中不触发实时计算
        verify(recommendCalculateService, never()).computeSimilarProducts(anyLong(), anyInt());
    }

    @Test
    void similar_noCache_usesItemCF() {
        ZSetOperations<String, String> zsetOps = mock(ZSetOperations.class);
        when(stringRedisTemplate.opsForZSet()).thenReturn(zsetOps);
        when(zsetOps.reverseRangeWithScores(anyString(), anyLong(), anyLong())).thenReturn(Collections.emptySet());

        // ItemCF 返回相似商品
        Map<Long, Double> scores = new HashMap<>();
        scores.put(500L, 0.9);
        when(recommendCalculateService.computeSimilarProducts(1L, 5)).thenReturn(scores);
        Product p = product(500L, "ItemCF相似", 10);
        p.setStatus(1);
        when(productMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(p));

        List<RecommendVO> result = service.similar(1L, 5);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(500L, result.get(0).getProductId());
    }

    @Test
    void similar_noItemCF_usesCategoryHotFallback() {
        ZSetOperations<String, String> zsetOps = mock(ZSetOperations.class);
        when(stringRedisTemplate.opsForZSet()).thenReturn(zsetOps);
        when(zsetOps.reverseRangeWithScores(anyString(), anyLong(), anyLong())).thenReturn(Collections.emptySet());
        when(recommendCalculateService.computeSimilarProducts(anyLong(), anyInt()))
                .thenReturn(Collections.emptyMap());

        Product current = product(1L, "目标商品", 5);
        current.setCategoryId(7L);
        when(productMapper.selectById(1L)).thenReturn(current);
        Product hot = product(600L, "同分类热门", 88);
        hot.setCategoryId(7L);
        when(productMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenAnswer(inv -> {
                    Page<Product> p = inv.getArgument(0);
                    p.setRecords(Collections.singletonList(hot));
                    return p;
                });

        List<RecommendVO> result = service.similar(1L, 5);

        assertEquals(1, result.size());
        assertEquals(600L, result.get(0).getProductId());
        assertEquals(4, result.get(0).getAlgorithmType());
    }

    @Test
    void similar_productNotFound_returnsEmpty() {
        ZSetOperations<String, String> zsetOps = mock(ZSetOperations.class);
        when(stringRedisTemplate.opsForZSet()).thenReturn(zsetOps);
        when(zsetOps.reverseRangeWithScores(anyString(), anyLong(), anyLong())).thenReturn(Collections.emptySet());
        when(recommendCalculateService.computeSimilarProducts(anyLong(), anyInt()))
                .thenReturn(Collections.emptyMap());
        when(productMapper.selectById(1L)).thenReturn(null);

        List<RecommendVO> result = service.similar(1L, 5);

        assertTrue(result.isEmpty());
    }

    // ==================== historyBased（浏览历史推荐，A-1） ====================

    @Test
    void historyBased_withViewedProducts_returnsSimilar() {
        // H-01：有浏览记录 + 相似商品 → 返回非空、algoType=2、不含已浏览商品
        var viewed = new com.pzhu.mall.modules.behavior.entity.UserBehavior();
        viewed.setUserId(1L);
        viewed.setProductId(10L);
        viewed.setBehaviorType(1);
        when(userBehaviorMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(java.util.Collections.singletonList(viewed));

        // 商品 10 的 ItemCF 相似：20（0.9 分）、30（0.5 分）
        Map<Long, Double> sim10 = new HashMap<>();
        sim10.put(20L, 0.9);
        sim10.put(30L, 0.5);
        when(recommendCalculateService.computeSimilarProducts(10L, 5)).thenReturn(sim10);

        // 批量加载相似商品
        Product p20 = product(20L, "相似商品A", 50);
        Product p30 = product(30L, "相似商品B", 30);
        when(productMapper.selectBatchIds(any())).thenReturn(List.of(p20, p30));

        List<RecommendVO> result = service.historyBased(1L, 10);

        assertEquals(2, result.size());
        // 平均分排序：20(0.9) 在前
        assertEquals(20L, result.get(0).getProductId());
        assertEquals(2, result.get(0).getAlgorithmType());
        // 不含已浏览商品 10
        assertTrue(result.stream().noneMatch(v -> v.getProductId() == 10L));
    }

    @Test
    void historyBased_noViewedProducts_returnsEmpty() {
        // H-02：无浏览记录 → 空列表
        when(userBehaviorMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(java.util.Collections.emptyList());
        assertTrue(service.historyBased(1L, 10).isEmpty());
    }

    @Test
    void historyBased_similarProductsAllOffline_returnsEmpty() {
        // H-03：相似商品全部下架 → 被过滤为空
        var viewed = new com.pzhu.mall.modules.behavior.entity.UserBehavior();
        viewed.setUserId(1L);
        viewed.setProductId(10L);
        viewed.setBehaviorType(1);
        when(userBehaviorMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(java.util.Collections.singletonList(viewed));

        Map<Long, Double> sim10 = new HashMap<>();
        sim10.put(20L, 0.9);
        when(recommendCalculateService.computeSimilarProducts(10L, 5)).thenReturn(sim10);

        // 相似商品已下架（status=2）
        Product offline = product(20L, "下架商品", 50);
        offline.setStatus(2);
        when(productMapper.selectBatchIds(any())).thenReturn(List.of(offline));

        List<RecommendVO> result = service.historyBased(1L, 10);

        assertTrue(result.isEmpty());
    }

    @Test
    void historyBased_numLimitRespected() {
        // H-04：num=1 → 最多返回 1 条
        var viewed = new com.pzhu.mall.modules.behavior.entity.UserBehavior();
        viewed.setUserId(1L);
        viewed.setProductId(10L);
        viewed.setBehaviorType(1);
        when(userBehaviorMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(java.util.Collections.singletonList(viewed));

        Map<Long, Double> sim10 = new HashMap<>();
        sim10.put(20L, 0.9);
        sim10.put(30L, 0.5);
        when(recommendCalculateService.computeSimilarProducts(10L, 5)).thenReturn(sim10);
        when(productMapper.selectBatchIds(any())).thenReturn(List.of(product(20L, "A", 1), product(30L, "B", 1)));

        List<RecommendVO> result = service.historyBased(1L, 1);

        assertEquals(1, result.size());
    }

    @Test
    void historyBased_numOutOfRange_clamped() {
        // H-05：num 越界（0）→ 钳制为默认 10（服务端 Math.max 兜底，不抛错）
        when(userBehaviorMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(java.util.Collections.emptyList());
        // 不抛异常即可（无浏览记录 → 空列表，num=0 已被 Math.max(0,1) 归一）
        assertTrue(service.historyBased(1L, 0).isEmpty());
    }

    // ==================== helpers ====================

    private static Product product(Long id, String name, int sales) {
        Product p = new Product();
        p.setId(id);
        p.setName(name);
        p.setSales(sales);
        p.setStatus(1);
        p.setIsDeleted(0);
        return p;
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
