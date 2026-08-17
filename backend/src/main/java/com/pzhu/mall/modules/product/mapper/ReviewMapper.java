package com.pzhu.mall.modules.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pzhu.mall.modules.product.entity.Review;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReviewMapper extends BaseMapper<Review> {

    /**
     * 查询某商品下的评价列表（按时间倒序）。
     */
    List<Review> listByProductId(@Param("productId") Long productId);

    /**
     * 统计某商品的平均评分。
     */
    Double avgRatingByProductId(@Param("productId") Long productId);

    /**
     * 统计某商品评价数量。
     */
    Long countByProductId(@Param("productId") Long productId);

    /**
     * FRONT-QA-02 修复：批量查询多个商品的平均评分（商品列表/推荐列表填充真实评分）。
     *
     * @return 每行包含 productId（Long）与 avgRating（Double）键的 Map 列表
     */
    List<java.util.Map<String, Object>> selectAvgRatingByProductIds(@Param("productIds") List<Long> productIds);
}
