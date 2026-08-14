package com.pzhu.mall.modules.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.common.result.PageResult;
import com.pzhu.mall.modules.order.entity.Order;
import com.pzhu.mall.modules.order.entity.OrderItem;
import com.pzhu.mall.modules.order.mapper.OrderItemMapper;
import com.pzhu.mall.modules.order.mapper.OrderMapper;
import com.pzhu.mall.modules.product.entity.Review;
import com.pzhu.mall.modules.product.mapper.ReviewMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品评价服务。
 * <p>评分按需动态计算，不写回 product 表（product 表无 rating 字段）。</p>
 */
@Service
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    @Resource
    private ReviewMapper reviewMapper;

    @Resource
    private OrderItemMapper orderItemMapper;

    @Resource
    private OrderMapper orderMapper;

    /**
     * 提交评价。
     */
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long orderItemId, Long userId, Integer rating, String content, String imagesJson) {
        // O-10/P-11 修复：评分必须为 1~5 的整数，越界直接拒绝（此前任意值可入库）
        if (rating == null || rating < 1 || rating > 5) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "评分必须在 1~5 之间");
        }
        OrderItem item = orderItemMapper.selectById(orderItemId);
        if (item == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        // 校验订单项归属当前用户（IDOR 防护）
        Order order = orderMapper.selectById(item.getOrderId());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权评价该订单项");
        }

        // M-16 修复：仅"已收货"(3)或"已完成"(4)的订单允许评价，防止未付款/未收货即评价
        Integer orderStatus = order.getStatus();
        if (orderStatus == null || (orderStatus != 3 && orderStatus != 4)) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "订单未完成，无法评价");
        }

        // 幂等：同一 orderItemId 只能评价一次（数据库 UNIQUE 约束兜底）
        Long existing = reviewMapper.selectCount(
                new LambdaQueryWrapper<Review>().eq(Review::getOrderItemId, orderItemId)
        );
        if (existing > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该订单项已评价");
        }

        Review review = new Review();
        review.setOrderItemId(orderItemId);
        review.setUserId(userId);
        review.setProductId(item.getProductId());
        review.setRating(rating);
        review.setContent(content);
        review.setImages(imagesJson);
        review.setCreateTime(LocalDateTime.now());
        try {
            reviewMapper.insert(review);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该订单项已评价");
        }

        log.info("[评价] 用户{}评价订单项{} 评分{} 商品{}", userId, orderItemId, rating, item.getProductId());
    }

    /**
     * 查询商品评价列表（分页）。
     * <p>H-22 修复：原实现 selectList 全量返回，热门商品评价量大时响应膨胀；改为分页查询。</p>
     */
    public PageResult<Review> listByProduct(Long productId, long pageNum, long pageSize) {
        Page<Review> page = new Page<>(pageNum, pageSize);
        Page<Review> result = reviewMapper.selectPage(page,
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getProductId, productId)
                        .orderByDesc(Review::getCreateTime)
        );
        return new PageResult<>(result.getTotal(), pageNum, pageSize, result.getPages(), result.getRecords());
    }

    /**
     * 获取商品平均评分与评价数（动态计算，不依赖 product 表字段）。
     */
    public ProductRatingVO getProductRating(Long productId) {
        Double avg = reviewMapper.avgRatingByProductId(productId);
        Long count = reviewMapper.countByProductId(productId);
        ProductRatingVO vo = new ProductRatingVO();
        vo.setProductId(productId);
        vo.setAvgRating(avg != null ? BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        vo.setReviewCount(count != null ? count.intValue() : 0);
        return vo;
    }

    /**
     * 评价统计 VO。
     */
    public static class ProductRatingVO {
        private Long productId;
        private BigDecimal avgRating;
        private Integer reviewCount;

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public BigDecimal getAvgRating() { return avgRating; }
        public void setAvgRating(BigDecimal avgRating) { this.avgRating = avgRating; }
        public Integer getReviewCount() { return reviewCount; }
        public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }
    }
}
