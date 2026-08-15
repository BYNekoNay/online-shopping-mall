package com.pzhu.mall.modules.product.controller;

import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.modules.product.service.CategoryService;
import com.pzhu.mall.modules.product.service.ProductService;
import com.pzhu.mall.modules.product.vo.ProductVO;
import com.pzhu.mall.security.LoginUserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ProductController 单元测试（E-1 覆盖率补测：product.controller 37% → ≥70%）。
 * <p>覆盖 PC-01~PC-05：列表/搜索历史/清空/详情。</p>
 */
class ProductControllerTest {

    private ProductService productService;
    private CategoryService categoryService;
    private ProductController controller;

    @BeforeEach
    void setUp() {
        productService = mock(ProductService.class);
        categoryService = mock(CategoryService.class);
        controller = new ProductController();
        inject(controller, "productService", productService);
        inject(controller, "categoryService", categoryService);
    }

    @AfterEach
    void tearDown() {
        LoginUserContext.clear();
    }

    @Test
    void list_returnsPage() {
        // PC-01：商品列表委托 service
        var query = new com.pzhu.mall.modules.product.dto.ProductQueryDTO();
        query.setKeyword("手机");
        query.setMinPrice(new BigDecimal("100"));
        query.setMaxPrice(new BigDecimal("500"));
        var vo = new ProductVO();
        vo.setId(1L);
        vo.setName("手机A");
        when(productService.listPage(any())).thenReturn(
                new com.pzhu.mall.common.result.PageResult<>(1L, 1, 10, 1L, List.of(vo)));

        Result<?> result = controller.list(query);

        assertEquals(0, result.getCode());
        verify(productService).listPage(argThat(q ->
                "手机".equals(q.getKeyword())
                        && q.getMinPrice().compareTo(new BigDecimal("100")) == 0
                        && q.getMaxPrice().compareTo(new BigDecimal("500")) == 0));
    }

    @Test
    void search_alias_delegatesToList() {
        // PC-02：/search 别名等价 list
        var query = new com.pzhu.mall.modules.product.dto.ProductQueryDTO();
        when(productService.listPage(any())).thenReturn(
                new com.pzhu.mall.common.result.PageResult<>(0L, 1, 10, 0L, List.of()));

        Result<?> r = controller.search(query);

        assertEquals(0, r.getCode());
        verify(productService).listPage(query);
    }

    @Test
    void searchHistory_loggedIn_returnsKeywords() {
        // PC-03：搜索历史（登录）→ 返回列表
        LoginUserContext.set(100L, 1);
        when(productService.listSearchHistory(100L, 10)).thenReturn(List.of("手机", "耳机"));

        Result<List<String>> r = controller.searchHistory(10);

        assertEquals(2, r.getData().size());
    }

    @Test
    void searchHistory_anonymous_returnsEmpty() {
        // PC-04：搜索历史（未登录）→ 空
        Result<List<String>> r = controller.searchHistory(10);
        assertEquals(0, r.getCode());
        assertTrue(r.getData().isEmpty());
    }

    @Test
    void clearSearchHistory_delegates() {
        // PC-05：清空搜索历史（登录）
        LoginUserContext.set(100L, 1);
        Result<Void> r = controller.clearSearchHistory();
        assertEquals(0, r.getCode());
        verify(productService).clearSearchHistory(100L);
    }

    @Test
    void detail_delegates() {
        // PC-06：商品详情（消费者视角校验 ONLINE）
        var vo = new ProductVO();
        vo.setId(1L);
        when(productService.getDetail(1L, true)).thenReturn(vo);

        Result<ProductVO> r = controller.detail(1L);

        assertEquals(1L, r.getData().getId());
        verify(productService).getDetail(1L, true);
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
