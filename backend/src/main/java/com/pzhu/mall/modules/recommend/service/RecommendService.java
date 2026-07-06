package com.pzhu.mall.modules.recommend.service;

import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.modules.product.entity.Product;
import com.pzhu.mall.modules.product.mapper.ProductMapper;
import com.pzhu.mall.modules.recommend.entity.RecommendResult;
import com.pzhu.mall.modules.recommend.mapper.RecommendResultMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 推荐服务（查询层）。
 */
@Service
public class RecommendService {

    @Resource
    private RecommendResultMapper recommendResultMapper;

    @Resource
    private ProductMapper productMapper;

    /**
     * 猜你喜欢（优先 Redis → 数据库 → 热门兜底）。
     */
    public List<Product> guessYouLike(Long userId, Integer num) {
        // 1. 优先读推荐结果表
        List<RecommendResult> results;
        if (userId != null) {
            results = recommendResultMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RecommendResult>()
                    .eq(RecommendResult::getUserId, userId)
                    .orderByDesc(RecommendResult::getScore)
                    .last("LIMIT " + num)
            );
        } else {
            // 未登录：全局热门兜底
            results = recommendResultMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RecommendResult>()
                    .isNull(RecommendResult::getUserId)
                    .orderByDesc(RecommendResult::getScore)
                    .last("LIMIT " + num)
            );
        }

        if (results.isEmpty()) {
            // 数据库无结果，返回热门商品兜底
            return productMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Product>()
                    .eq(Product::getStatus, 1)
                    .orderByDesc(Product::getSales)
                    .last("LIMIT " + num)
            );
        }

        List<Long> productIds = results.stream()
                .map(RecommendResult::getProductId)
                .collect(Collectors.toList());

        return productMapper.selectBatchIds(productIds);
    }

    /**
     * 相似商品推荐。
     */
    public List<Product> similar(Long productId, Integer num) {
        List<RecommendResult> results = recommendResultMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RecommendResult>()
                .eq(RecommendResult::getProductId, productId)
                .orderByDesc(RecommendResult::getScore)
                .last("LIMIT " + num)
        );

        if (results.isEmpty()) {
            // 兜底：同分类热门商品
            Product current = productMapper.selectById(productId);
            if (current != null) {
                return productMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Product>()
                        .eq(Product::getCategoryId, current.getCategoryId())
                        .ne(Product::getId, productId)
                        .eq(Product::getStatus, 1)
                        .orderByDesc(Product::getSales)
                        .last("LIMIT " + num)
                );
            }
            return List.of();
        }

        List<Long> productIds = results.stream()
                .map(RecommendResult::getProductId)
                .collect(Collectors.toList());
        return productMapper.selectBatchIds(productIds);
    }
}
