package com.pzhu.mall.modules.product.service;

import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.modules.order.entity.OrderItem;
import com.pzhu.mall.modules.order.mapper.OrderItemMapper;
import com.pzhu.mall.modules.product.entity.Review;
import com.pzhu.mall.modules.product.mapper.ReviewMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReviewServiceTest {

    @Test
    void submit_success() {
        ReviewMapper reviewMapper = mock(ReviewMapper.class);
        OrderItemMapper orderItemMapper = mock(OrderItemMapper.class);
        ReviewService service = new ReviewService();
        // inject mocks via reflection
        inject(service, "reviewMapper", reviewMapper);
        inject(service, "orderItemMapper", orderItemMapper);

        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setProductId(10L);
        when(orderItemMapper.selectById(1L)).thenReturn(item);
        when(reviewMapper.selectCount(any())).thenReturn(0L);
        when(reviewMapper.insert(any())).thenReturn(1);

        service.submit(1L, 100L, 5, "good", null);

        verify(reviewMapper).insert(any(Review.class));
    }

    @Test
    void submit_duplicate_throws() {
        ReviewMapper reviewMapper = mock(ReviewMapper.class);
        OrderItemMapper orderItemMapper = mock(OrderItemMapper.class);
        ReviewService service = new ReviewService();
        inject(service, "reviewMapper", reviewMapper);
        inject(service, "orderItemMapper", orderItemMapper);

        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setProductId(10L);
        when(orderItemMapper.selectById(1L)).thenReturn(item);
        when(reviewMapper.selectCount(any())).thenReturn(1L);

        assertThrows(BusinessException.class, () -> service.submit(1L, 100L, 5, "good", null));
    }

    @Test
    void getProductRating_noReviews_returnsZero() {
        ReviewMapper reviewMapper = mock(ReviewMapper.class);
        ReviewService service = new ReviewService();
        inject(service, "reviewMapper", reviewMapper);

        when(reviewMapper.avgRatingByProductId(10L)).thenReturn(null);
        when(reviewMapper.countByProductId(10L)).thenReturn(null);

        var vo = service.getProductRating(10L);
        assertEquals(BigDecimal.ZERO, vo.getAvgRating());
        assertEquals(0, vo.getReviewCount());
    }

    @Test
    void getProductRating_withReviews_returnsCorrect() {
        ReviewMapper reviewMapper = mock(ReviewMapper.class);
        ReviewService service = new ReviewService();
        inject(service, "reviewMapper", reviewMapper);

        when(reviewMapper.avgRatingByProductId(10L)).thenReturn(4.5);
        when(reviewMapper.countByProductId(10L)).thenReturn(10L);

        var vo = service.getProductRating(10L);
        assertEquals(new BigDecimal("4.5"), vo.getAvgRating());
        assertEquals(10, vo.getReviewCount());
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
