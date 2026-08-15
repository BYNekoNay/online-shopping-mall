package com.pzhu.mall.modules.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.common.result.PageResult;
import com.pzhu.mall.modules.behavior.service.BehaviorService;
import com.pzhu.mall.modules.marketing.service.PromotionService;
import com.pzhu.mall.modules.product.dto.ProductQueryDTO;
import com.pzhu.mall.modules.product.entity.Category;
import com.pzhu.mall.modules.product.entity.Product;
import com.pzhu.mall.modules.product.entity.Sku;
import com.pzhu.mall.modules.product.mapper.CategoryMapper;
import com.pzhu.mall.modules.product.mapper.ProductMapper;
import com.pzhu.mall.modules.product.mapper.SkuMapper;
import com.pzhu.mall.modules.statistics.mapper.SearchHistoryMapper;
import com.pzhu.mall.modules.product.vo.ProductVO;
import com.pzhu.mall.security.LoginUserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ProductService 单元测试（商品创建/详情状态机/列表搜索）。
 * <p>覆盖 docs/32 批次1 的 P-T01~05 用例：P-01 直链校验、P-03 行为视角区分、createWithSkus。</p>
 */
class ProductServiceTest {

    private ProductMapper productMapper;
    private CategoryMapper categoryMapper;
    private SkuMapper skuMapper;
    private SearchHistoryMapper searchHistoryMapper;
    private BehaviorService behaviorService;
    private PromotionService promotionService;
    private ProductService service;

    @BeforeEach
    void setUp() {
        productMapper = mock(ProductMapper.class);
        categoryMapper = mock(CategoryMapper.class);
        skuMapper = mock(SkuMapper.class);
        searchHistoryMapper = mock(SearchHistoryMapper.class);
        behaviorService = mock(BehaviorService.class);
        promotionService = mock(PromotionService.class);
        service = new ProductService();
        inject(service, "productMapper", productMapper);
        inject(service, "categoryMapper", categoryMapper);
        inject(service, "skuMapper", skuMapper);
        inject(service, "searchHistoryMapper", searchHistoryMapper);
        inject(service, "behaviorService", behaviorService);
        inject(service, "promotionService", promotionService);
        when(promotionService.matchActive(any())).thenReturn(Collections.emptyList());
    }

    @AfterEach
    void tearDown() {
        LoginUserContext.clear();
    }

    // ==================== createWithSkus ====================

    @Test
    void createWithSkus_insertsProductAndSkus() {
        Product p = new Product();
        p.setId(10L);
        Sku s1 = new Sku();
        Sku s2 = new Sku();
        when(productMapper.insert(any())).thenReturn(1);

        Product result = service.createWithSkus(p, Arrays.asList(s1, s2));

        assertEquals(10L, result.getId());
        verify(productMapper).insert(p);
        verify(skuMapper, times(2)).insert(any(Sku.class));
        // SKU 绑定商品 id 且未删除
        assertEquals(10L, s1.getProductId());
        assertEquals(Integer.valueOf(0), s1.getIsDeleted());
    }

    @Test
    void createWithSkus_nullSkus_insertsProductOnly() {
        Product p = new Product();
        p.setId(11L);

        service.createWithSkus(p, null);

        verify(productMapper).insert(p);
        verify(skuMapper, never()).insert(any(Sku.class));
    }

    // ==================== getDetail（P-01 直链校验 / P-03 行为视角） ====================

    @Test
    void getDetail_productNotFound_throws() {
        when(productMapper.selectById(1L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.getDetail(1L, true));
    }

    @Test
    void getDetail_deletedProduct_throws() {
        Product p = new Product();
        p.setId(1L);
        p.setIsDeleted(1);
        when(productMapper.selectById(1L)).thenReturn(p);

        assertThrows(BusinessException.class, () -> service.getDetail(1L, true));
    }

    @Test
    void getDetail_consumerView_offlineProduct_throws() {
        // P-01 修复验证：消费者视角直链访问下架/待审核商品 → PRODUCT_OFFLINE_ORDER
        Product p = product(1L, 2); // status=2 下架
        when(productMapper.selectById(1L)).thenReturn(p);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.getDetail(1L, true));
        assertEquals(ErrorCode.PRODUCT_OFFLINE_ORDER.getCode(), ex.getCode());
        // 下架商品不记浏览行为
        verify(behaviorService, never()).record(anyLong(), anyLong(), anyInt());
    }

    @Test
    void getDetail_consumerView_online_recordsBehavior() {
        // P-03 修复验证：仅消费者视角且已登录才记录浏览行为
        LoginUserContext.set(100L, 1);
        Product p = product(1L, 1); // status=1 在售
        when(productMapper.selectById(1L)).thenReturn(p);

        ProductVO vo = service.getDetail(1L, true);

        assertNotNull(vo);
        verify(behaviorService).record(100L, 1L, 1);
    }

    @Test
    void getDetail_merchantView_online_noBehavior() {
        // P-03 修复验证：商家/管理端视角不记录浏览行为（不污染推荐矩阵）
        LoginUserContext.set(200L, 2);
        Product p = product(1L, 1);
        when(productMapper.selectById(1L)).thenReturn(p);

        service.getDetail(1L, false);

        verify(behaviorService, never()).record(anyLong(), anyLong(), anyInt());
    }

    @Test
    void getDetail_consumerView_online_anonymous_noBehavior() {
        // 匿名（未登录）消费者看详情：不记行为（currentUserId 为 null）
        Product p = product(1L, 1);
        when(productMapper.selectById(1L)).thenReturn(p);

        service.getDetail(1L, true);

        verify(behaviorService, never()).record(anyLong(), anyLong(), anyInt());
    }

    @Test
    void getDetail_injectsActivePromotion() {
        Product p = product(1L, 1);
        p.setShopId(5L);
        when(productMapper.selectById(1L)).thenReturn(p);
        com.pzhu.mall.modules.marketing.entity.Promotion promo = new com.pzhu.mall.modules.marketing.entity.Promotion();
        promo.setId(9L);
        when(promotionService.matchActive(5L)).thenReturn(Collections.singletonList(promo));

        ProductVO vo = service.getDetail(1L, true);

        assertNotNull(vo.getActivePromotion());
        assertEquals(9L, ((com.pzhu.mall.modules.marketing.entity.Promotion) vo.getActivePromotion()).getId());
    }

    // ==================== listPage（搜索 + 历史记录） ====================

    @Test
    void listPage_keywordFirstPage_recordsSearchHistory() {
        ProductQueryDTO query = new ProductQueryDTO();
        query.setPageNum(1);
        query.setPageSize(10);
        query.setKeyword("手机");
        Page<Product> page = new Page<>(1, 10);
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        when(productMapper.selectPage(any(), any())).thenReturn(page);

        PageResult<ProductVO> result = service.listPage(query);

        assertNotNull(result);
        verify(searchHistoryMapper).insert(any());
    }

    @Test
    void listPage_keywordLaterPage_noDuplicateHistory() {
        // P-08 修复验证：翻页（pageNum>1）不再重复记录搜索历史
        ProductQueryDTO query = new ProductQueryDTO();
        query.setPageNum(3);
        query.setPageSize(10);
        query.setKeyword("手机");
        Page<Product> page = new Page<>(1, 10);
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        when(productMapper.selectPage(any(), any())).thenReturn(page);

        service.listPage(query);

        verify(searchHistoryMapper, never()).insert(any());
    }

    @Test
    void listPage_filtersByStatusAndPrice() {
        ProductQueryDTO query = new ProductQueryDTO();
        query.setPageNum(1);
        query.setPageSize(10);
        query.setStatus(1);
        query.setMinPrice(new BigDecimal("100"));
        query.setMaxPrice(new BigDecimal("500"));
        Page<Product> page = new Page<>(1, 10);
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        when(productMapper.selectPage(any(), any())).thenReturn(page);

        service.listPage(query);

        verify(productMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    // ==================== toVOList（AD-04 批量构建） ====================

    @Test
    void toVOList_nullOrEmpty_returnsEmpty() {
        assertTrue(service.toVOList(null).isEmpty());
        assertTrue(service.toVOList(Collections.emptyList()).isEmpty());
        verify(categoryMapper, never()).selectBatchIds(any());
    }

    @Test
    void toVOList_buildsWithCategoryAndSku() {
        Product p1 = product(1L, 1);
        p1.setCategoryId(7L);
        Product p2 = product(2L, 1);
        p2.setCategoryId(null);
        Category cat = new Category();
        cat.setId(7L);
        cat.setName("数码");
        Sku sku = new Sku();
        sku.setId(11L);
        sku.setProductId(1L);
        when(categoryMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(cat));
        when(skuMapper.selectList(any())).thenReturn(Collections.singletonList(sku));

        var vos = service.toVOList(Arrays.asList(p1, p2));

        assertEquals(2, vos.size());
        // p1 带分类名
        assertEquals("数码", vos.get(0).getCategoryName());
        // p2 无分类
        assertNull(vos.get(1).getCategoryName());
        // 批量加载了 SKU（toVOList 与 toVO 内部各查一次）
        verify(skuMapper, atLeastOnce()).selectList(any());
    }

    // ==================== D-3 搜索历史 ====================

    @Test
    void listSearchHistory_deduplicatesKeywords() {
        // SH-01：去重 + 按时间倒序
        var h1 = new com.pzhu.mall.modules.statistics.entity.SearchHistory();
        h1.setUserId(100L); h1.setKeyword("手机");
        var h2 = new com.pzhu.mall.modules.statistics.entity.SearchHistory();
        h2.setUserId(100L); h2.setKeyword("手机");
        var h3 = new com.pzhu.mall.modules.statistics.entity.SearchHistory();
        h3.setUserId(100L); h3.setKeyword("耳机");
        when(searchHistoryMapper.selectList(any())).thenReturn(List.of(h1, h2, h3));

        List<String> result = service.listSearchHistory(100L, 10);

        assertEquals(2, result.size());
        assertTrue(result.contains("手机"));
        assertTrue(result.contains("耳机"));
    }

    @Test
    void listSearchHistory_anonymous_returnsEmpty() {
        // SH-02：未登录（userId=null）→ 空列表
        assertTrue(service.listSearchHistory(null, 10).isEmpty());
        verify(searchHistoryMapper, never()).selectList(any());
    }

    @Test
    void clearSearchHistory_deletesByUser() {
        // SH-03：清空按用户删除
        service.clearSearchHistory(100L);
        verify(searchHistoryMapper).delete(any());
    }

    // ==================== helpers ====================

    private static Product product(Long id, Integer status) {
        Product p = new Product();
        p.setId(id);
        p.setName("商品" + id);
        p.setPrice(new BigDecimal("99.00"));
        p.setStock(10);
        p.setSales(0);
        p.setStatus(status);
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
