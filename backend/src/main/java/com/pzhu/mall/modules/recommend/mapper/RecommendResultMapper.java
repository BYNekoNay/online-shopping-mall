package com.pzhu.mall.modules.recommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pzhu.mall.modules.recommend.entity.RecommendResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface RecommendResultMapper extends BaseMapper<RecommendResult> {

    /**
     * M-13 修复：推荐曝光分母——统计时间窗口内去重后的（用户, 商品）推荐曝光对数。
     * <p>user_id 为 NULL 的全局热门兜底行不参与 CTR 计算（COUNT(DISTINCT) 自动忽略 NULL）。
     */
    @Select("SELECT COUNT(DISTINCT user_id, product_id) FROM recommend_result WHERE generate_time >= #{since}")
    long countDistinctExposure(@Param("since") LocalDateTime since);

    /**
     * M-13 修复：推荐点击分子——统计时间窗口内"先被推荐、后被浏览"的去重（用户, 商品）对数。
     * <p>通过 EXISTS 关联 recommend_result 做推荐归因，只计入确实由推荐位触发的浏览，
     * 避免把全站浏览（含搜索、直接访问）都算作推荐点击导致指标虚高。
     */
    @Select("SELECT COUNT(DISTINCT ub.user_id, ub.product_id) FROM user_behavior ub " +
            "WHERE ub.behavior_type = 1 AND ub.create_time >= #{since} " +
            "AND EXISTS (SELECT 1 FROM recommend_result rr " +
            "WHERE rr.user_id = ub.user_id AND rr.product_id = ub.product_id " +
            "AND rr.generate_time >= #{since})")
    long countDistinctRecommendClick(@Param("since") LocalDateTime since);
}
