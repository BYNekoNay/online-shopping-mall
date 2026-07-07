package com.pzhu.mall.modules.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pzhu.mall.common.result.PageResult;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.common.enums.ProductStatus;
import com.pzhu.mall.modules.product.dto.ProductQueryDTO;
import com.pzhu.mall.modules.product.entity.Category;
import com.pzhu.mall.modules.product.entity.Product;
import com.pzhu.mall.modules.product.mapper.ProductMapper;
import com.pzhu.mall.modules.product.mapper.CategoryMapper;
import com.pzhu.mall.modules.behavior.service.BehaviorService;
import com.pzhu.mall.modules.product.vo.ProductVO;
import com.pzhu.mall.modules.product.vo.SkuVO;
import com.pzhu.mall.modules.product.vo.CategoryVO;
import com.pzhu.mall.modules.product.mapper.SkuMapper;
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
    private SkuMapper skuMapper;

    @Resource
    private SearchHistoryMapper searchHistoryMapper;

    @Resource
    private BehaviorService behaviorService;

    @Resource
    private com.pzhu.mall.modules.marketing.service.PromotionService promotionService;

    public ProductMapper getProductMapper() {
        return productMapper;
    }

    public SkuMapper getSkuMapper() {
        return skuMapper;
    }

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
        if ("sales".equals(query.getSort())) {
            qw.orderByDesc(Product::getSales);
        } else if ("price_asc".equals(query.getSort())) {
            qw.orderByAsc(Product::getPrice);
        } else if ("price_desc".equals(query.getSort())) {
            qw.orderByDesc(Product::getPrice);
        } else if ("new".equals(query.getSort())) {
            qw.orderByDesc(Product::getCreateTime);
        }

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
        ProductVO vo = toVO(product);

        // 注入当前命中的生效促销
        List<com.pzhu.mall.modules.marketing.entity.Promotion> promotions = promotionService.matchActive(product.getShopId());
        if (!promotions.isEmpty()) {
            vo.setActivePromotion(promotions.get(0));
        }

        // 记录浏览行为（behaviorType=1）
        Long currentUserId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();
        if (currentUserId != null) {
            behaviorService.record(currentUserId, productId, 1);
        }

        return vo;
    }

    public ProductVO toVO(Product product) {
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

        // Load category name
        Category category = categoryMapper.selectById(product.getCategoryId());
        if (category != null) {
            vo.setCategoryName(category.getName());
        }

        // Load sku list
        var skuQw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.pzhu.mall.modules.product.entity.Sku>();
        skuQw.eq(com.pzhu.mall.modules.product.entity.Sku::getProductId, product.getId())
             .eq(com.pzhu.mall.modules.product.entity.Sku::getIsDeleted, 0);
        List<com.pzhu.mall.modules.product.entity.Sku> skus = skuMapper.selectList(skuQw);
        if (skus != null) {
            vo.setSkuList(skus.stream().map(sku -> {
                SkuVO sv = new SkuVO();
                sv.setId(sku.getId());
                sv.setProductId(sku.getProductId());
                sv.setSpecJson(sku.getSpecJson());
                sv.setPrice(sku.getPrice());
                sv.setStock(sku.getStock());
                sv.setImage(sku.getImage());
                return sv;
            }).collect(java.util.stream.Collectors.toList()));
        }

        return vo;
    }

    private void recordSearchHistory(String keyword) {
        // 搜索历史记录（异步，暂直接调用）
        try {
            var history = new com.pzhu.mall.modules.statistics.entity.SearchHistory();
            Long currentUserId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();
            history.setUserId(currentUserId != null ? currentUserId : 0L);
            history.setKeyword(keyword);
            searchHistoryMapper.insert(history);
        } catch (Exception e) {
            // 搜索历史记录失败不影响主流程
        }
    }
}
