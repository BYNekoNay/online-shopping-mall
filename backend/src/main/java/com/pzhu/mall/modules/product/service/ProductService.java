package com.pzhu.mall.modules.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pzhu.mall.common.result.PageResult;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.modules.product.dto.ProductQueryDTO;
import com.pzhu.mall.modules.product.entity.Category;
import com.pzhu.mall.modules.product.entity.Product;
import com.pzhu.mall.modules.product.mapper.ProductMapper;
import com.pzhu.mall.modules.product.mapper.CategoryMapper;
import com.pzhu.mall.modules.product.vo.ProductVO;
import com.pzhu.mall.modules.product.vo.CategoryVO;
import com.pzhu.mall.modules.statistics.mapper.SearchHistoryMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 商品服务。
 */
@Service
public class ProductService {

    @Resource
    private ProductMapper productMapper;

    @Resource
    private CategoryMapper categoryMapper;

    @Resource
    private SearchHistoryMapper searchHistoryMapper;

    /**
     * 分页查询商品列表。
     */
    public PageResult<ProductVO> listPage(ProductQueryDTO query) {
        LambdaQueryWrapper<Product> qw = new LambdaQueryWrapper<>();
        qw.eq(Product::getIsDeleted, 0);

        if (query.getCategoryId() != null) {
            qw.eq(Product::getCategoryId, query.getCategoryId());
        }
        if (query.getShopId() != null) {
            qw.eq(Product::getShopId, query.getShopId());
        }
        if (query.getStatus() != null) {
            qw.eq(Product::getStatus, query.getStatus());
        }
        if (query.getKeyword() != null && !query.getKeyword().isEmpty()) {
            qw.like(Product::getName, query.getKeyword());
            // 异步记录搜索历史（简单直接调用，量级小无需消息队列）
            recordSearchHistory(query.getKeyword());
        }
        if (query.getMinPrice() != null) {
            qw.ge(Product::getPrice, query.getMinPrice());
        }
        if (query.getMaxPrice() != null) {
            qw.le(Product::getPrice, query.getMaxPrice());
        }
        qw.orderByDesc(Product::getCreateTime);

        Page<Product> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<Product> result = productMapper.selectPage(page, qw);

        List<ProductVO> voList = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        return new PageResult<>(result.getTotal(), query.getPageNum(), query.getPageSize(), result.getPages(), voList);
    }

    /**
     * 商品详情。
     */
    public ProductVO getDetail(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null || product.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return toVO(product);
    }

    private ProductVO toVO(Product product) {
        ProductVO vo = new ProductVO();
        vo.setId(product.getId());
        vo.setShopId(product.getShopId());
        vo.setCategoryId(product.getCategoryId());
        vo.setName(product.getName());
        vo.setMainImage(product.getMainImage());
        vo.setImages(product.getImages());
        vo.setDetail(product.getDetail());
        vo.setPrice(product.getPrice());
        vo.setOriginalPrice(product.getOriginalPrice());
        vo.setStock(product.getStock());
        vo.setSales(product.getSales());
        vo.setStatus(product.getStatus());
        vo.setCreateTime(product.getCreateTime());
        return vo;
    }

    private void recordSearchHistory(String keyword) {
        // 搜索历史记录（异步，暂直接调用）
        try {
            var history = new com.pzhu.mall.modules.statistics.entity.SearchHistory();
            history.setUserId(0L); // 未登录时记录为 0
            history.setKeyword(keyword);
            searchHistoryMapper.insert(history);
        } catch (Exception e) {
            // 搜索历史记录失败不影响主流程
        }
    }
}
