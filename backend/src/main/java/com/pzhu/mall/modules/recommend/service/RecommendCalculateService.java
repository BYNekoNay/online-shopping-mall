package com.pzhu.mall.modules.recommend.service;

import com.pzhu.mall.modules.product.entity.Product;
import com.pzhu.mall.modules.product.mapper.ProductMapper;
import com.pzhu.mall.modules.recommend.entity.RecommendResult;
import com.pzhu.mall.modules.recommend.mapper.RecommendResultMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 推荐结果离线计算服务（简化版）。
 *
 * <p>当前阶段仅保留框架，实际算法在模块8（推荐算法模块）中实现。
 */
@Service
public class RecommendCalculateService {

    @Resource
    private RecommendResultMapper recommendResultMapper;

    @Resource
    private ProductMapper productMapper;

    /**
     * 为指定用户计算推荐结果并写入数据库。
     */
    public void calculateForUser(Long userId) {
        // TODO: 模块8中实现 UserCF/ItemCF/混合策略
        // 当前阶段先写空结果，保证接口不报错
    }

    /**
     * 为所有用户重新计算推荐结果。
     */
    public void calculateForAll() {
        // TODO: 模块8中实现
    }
}
