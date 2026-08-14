package com.pzhu.mall.modules.product.controller;

import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.common.result.PageResult;
import com.pzhu.mall.modules.product.dto.ProductQueryDTO;
import com.pzhu.mall.modules.product.entity.Product;
import com.pzhu.mall.modules.product.entity.Review;
import com.pzhu.mall.modules.product.service.ProductService;
import com.pzhu.mall.modules.product.service.CategoryService;
import com.pzhu.mall.modules.product.service.ReviewService;
import com.pzhu.mall.modules.product.vo.ProductVO;
import com.pzhu.mall.modules.product.vo.CategoryVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductControllerTest {

    @Test
    void detail_returnsProductVO() {
        ProductService productService = mock(ProductService.class);
        CategoryService categoryService = mock(CategoryService.class);
        ReviewService reviewService = mock(ReviewService.class);
        ProductController controller = new ProductController();
        inject(controller, "productService", productService);
        inject(controller, "categoryService", categoryService);
        inject(controller, "reviewService", reviewService);

        ProductVO vo = new ProductVO();
        vo.setId(1L);
        vo.setName("Test Product");
        // P-01/P-03 修复：消费者详情调用 getDetail(id, true)
        when(productService.getDetail(1L, true)).thenReturn(vo);

        var result = controller.detail(1L);
        assertEquals(vo, result.getData());
        verify(productService).getDetail(1L, true);
    }

    @Test
    void categories_returnsCategoryTree() {
        ProductService productService = mock(ProductService.class);
        CategoryService categoryService = mock(CategoryService.class);
        ReviewService reviewService = mock(ReviewService.class);
        ProductController controller = new ProductController();
        inject(controller, "productService", productService);
        inject(controller, "categoryService", categoryService);
        inject(controller, "reviewService", reviewService);

        List<CategoryVO> tree = List.of(new CategoryVO());
        when(categoryService.listTree()).thenReturn(tree);

        var result = controller.categories();
        assertEquals(tree, result.getData());
    }

    @Test
    void reviews_delegatesToReviewService() {
        ProductService productService = mock(ProductService.class);
        CategoryService categoryService = mock(CategoryService.class);
        ReviewService reviewService = mock(ReviewService.class);
        ProductController controller = new ProductController();
        inject(controller, "productService", productService);
        inject(controller, "categoryService", categoryService);
        inject(controller, "reviewService", reviewService);

        List<Review> reviews = List.of(new Review());
        // H-22 配套：评价列表改为分页返回
        PageResult<Review> pageResult = new PageResult<>(1L, 1L, 20L, 1L, reviews);
        when(reviewService.listByProduct(1L, 1L, 20L)).thenReturn(pageResult);

        var result = controller.reviews(1L, 1L, 20L);
        assertEquals(pageResult, result.getData());
    }

    @Test
    void rating_delegatesToReviewService() {
        ProductService productService = mock(ProductService.class);
        CategoryService categoryService = mock(CategoryService.class);
        ReviewService reviewService = mock(ReviewService.class);
        ProductController controller = new ProductController();
        inject(controller, "productService", productService);
        inject(controller, "categoryService", categoryService);
        inject(controller, "reviewService", reviewService);

        var ratingVO = new ReviewService.ProductRatingVO();
        ratingVO.setProductId(1L);
        ratingVO.setAvgRating(new BigDecimal("4.5"));
        ratingVO.setReviewCount(10);
        when(reviewService.getProductRating(1L)).thenReturn(ratingVO);

        var result = controller.rating(1L);
        var data = (ReviewService.ProductRatingVO) result.getData();
        assertEquals(new BigDecimal("4.5"), data.getAvgRating());
        assertEquals(10, data.getReviewCount());
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
