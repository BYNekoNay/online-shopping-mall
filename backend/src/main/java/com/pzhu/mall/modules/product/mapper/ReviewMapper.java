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
}
