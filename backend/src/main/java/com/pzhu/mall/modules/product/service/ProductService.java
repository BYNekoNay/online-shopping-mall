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
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 商品服务。
 */
@Service
public class ProductService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ProductService.class);

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
     * 创建商品及其 SKU 列表（事务保护）。
     */
    @Transactional(rollbackFor = Exception.class)
    public Product createWithSkus(Product product, List<com.pzhu.mall.modules.product.entity.Sku> skus) {
        productMapper.insert(product);
        if (skus != null && !skus.isEmpty()) {
            for (com.pzhu.mall.modules.product.entity.Sku sku : skus) {
                sku.setProductId(product.getId());
                sku.setIsDeleted(0);
                skuMapper.insert(sku);
            }
        }
        return product;
    }

    /**
     * 分页查询商品列表（批量加载 Category 和 SKU 消除 N+1）。
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
            // P-08 修复：仅首页搜索记录历史，翻页不再重复写入（此前每条分页都记录导致重复）
            if (query.getPageNum() == null || query.getPageNum() <= 1) {
                recordSearchHistory(query.getKeyword());
            }
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

        // m3 修复：批量加载 Category 和 SKU，消除 N+1 查询
        java.util.Set<Long> categoryIds = result.getRecords().stream()
                .map(Product::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        java.util.Map<Long, String> categoryNameMap = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            List<Category> categories = categoryMapper.selectBatchIds(categoryIds);
            for (Category c : categories) {
                categoryNameMap.put(c.getId(), c.getName());
            }
        }

        java.util.Set<Long> productIds = result.getRecords().stream()
                .map(Product::getId)
                .collect(Collectors.toSet());
        java.util.Map<Long, List<com.pzhu.mall.modules.product.entity.Sku>> skuMap;
        if (!productIds.isEmpty()) {
            var skuQw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.pzhu.mall.modules.product.entity.Sku>();
            skuQw.in(com.pzhu.mall.modules.product.entity.Sku::getProductId, productIds)
                 .eq(com.pzhu.mall.modules.product.entity.Sku::getIsDeleted, 0);
            List<com.pzhu.mall.modules.product.entity.Sku> allSkus = skuMapper.selectList(skuQw);
            skuMap = allSkus.stream()
                    .collect(Collectors.groupingBy(com.pzhu.mall.modules.product.entity.Sku::getProductId));
        } else {
            skuMap = new HashMap<>();
        }

        List<ProductVO> voList = result.getRecords().stream()
                .map(p -> toVO(p, categoryNameMap.get(p.getCategoryId()), skuMap.get(p.getId())))
                .collect(Collectors.toList());

        return new PageResult<>(result.getTotal(), query.getPageNum(), query.getPageSize(), result.getPages(), voList);
    }

    /**
     * 商品详情（消费者视角：校验 ONLINE + 记浏览行为）。
     * <p>P-01/P-03 修复：拆分消费者与商家/管理员视角——
     * 消费者端直链下架/待审核商品返回下架错误；商家/管理员查看自家商品详情放行且不记浏览行为（避免污染推荐矩阵）。</p>
     */
    public ProductVO getDetail(Long productId) {
        return getDetail(productId, false);
    }

    /**
     * 商品详情。
     *
     * @param consumerView true=消费者视角（校验 ONLINE、记浏览行为）；false=商家/管理端视角（放行、不记行为）
     */
    public ProductVO getDetail(Long productId, boolean consumerView) {
        Product product = productMapper.selectById(productId);
        if (product == null || product.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        // P-01 修复：消费者视角下，非上架商品（下架/待审核/已拒绝）通过直链访问返回下架错误
        if (consumerView && ProductStatus.of(product.getStatus()) != ProductStatus.ONLINE) {
            throw new BusinessException(ErrorCode.PRODUCT_OFFLINE_ORDER);
        }
        ProductVO vo = toVO(product);

        // 注入当前命中的生效促销
        List<com.pzhu.mall.modules.marketing.entity.Promotion> promotions = promotionService.matchActive(product.getShopId());
        if (!promotions.isEmpty()) {
            vo.setActivePromotion(promotions.get(0));
        }

        // P-03 修复：仅消费者视角记录浏览行为（商家/管理员看详情不污染推荐数据）
        if (consumerView) {
            Long currentUserId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();
            if (currentUserId != null) {
                behaviorService.record(currentUserId, productId, 1);
            }
        }

        return vo;
    }

    public ProductVO toVO(Product product) {
        return toVO(product, null, null);
    }

    /**
     * AD-04 修复：批量构建 VO（一次查询 category + sku，消除 N+1）。
     * 供管理员商品列表等场景使用。
     */
    public java.util.List<ProductVO> toVOList(java.util.List<Product> products) {
        if (products == null || products.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        java.util.Set<Long> categoryIds = products.stream()
                .map(Product::getCategoryId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        Map<Long, String> categoryNameMap = new java.util.HashMap<>();
        if (!categoryIds.isEmpty()) {
            categoryMapper.selectBatchIds(categoryIds)
                    .forEach(c -> categoryNameMap.put(c.getId(), c.getName()));
        }
        java.util.Set<Long> productIds = products.stream()
                .map(Product::getId)
                .collect(java.util.stream.Collectors.toSet());
        Map<Long, List<com.pzhu.mall.modules.product.entity.Sku>> skuMap = new java.util.HashMap<>();
        if (!productIds.isEmpty()) {
            var skuQw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.pzhu.mall.modules.product.entity.Sku>();
            skuQw.in(com.pzhu.mall.modules.product.entity.Sku::getProductId, productIds)
                    .eq(com.pzhu.mall.modules.product.entity.Sku::getIsDeleted, 0);
            skuMapper.selectList(skuQw).forEach(sku ->
                    skuMap.computeIfAbsent(sku.getProductId(), k -> new java.util.ArrayList<>()).add(sku));
        }
        return products.stream()
                .map(p -> toVO(p, categoryNameMap.get(p.getCategoryId()), skuMap.getOrDefault(p.getId(), null)))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * m3 修复：批量构建 VO，使用预加载的 categoryName 和 skuList，消除 N+1 查询。
     */
    ProductVO toVO(Product product, String preloadedCategoryName,
                   List<com.pzhu.mall.modules.product.entity.Sku> preloadedSkus) {
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

        // 使用预加载的 category 名称（若未预加载则回退单独查询）
        if (preloadedCategoryName != null) {
            vo.setCategoryName(preloadedCategoryName);
        } else {
            Category category = categoryMapper.selectById(product.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getName());
            }
        }

        // 使用预加载的 SKU 列表（若未预加载则回退单独查询）
        List<com.pzhu.mall.modules.product.entity.Sku> skus = preloadedSkus;
        if (skus == null) {
            var skuQw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.pzhu.mall.modules.product.entity.Sku>();
            skuQw.eq(com.pzhu.mall.modules.product.entity.Sku::getProductId, product.getId())
                 .eq(com.pzhu.mall.modules.product.entity.Sku::getIsDeleted, 0);
            skus = skuMapper.selectList(skuQw);
        }
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
        try {
            var history = new com.pzhu.mall.modules.statistics.entity.SearchHistory();
            Long currentUserId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();
            history.setUserId(currentUserId != null ? currentUserId : 0L);
            history.setKeyword(keyword);
            searchHistoryMapper.insert(history);
        } catch (Exception e) {
            log.warn("记录搜索历史失败, keyword={}", keyword, e);
        }
    }
}
