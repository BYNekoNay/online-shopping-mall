package com.pzhu.mall.modules.product.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.common.result.PageResult;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.common.enums.ProductStatus;
import com.pzhu.mall.modules.product.dto.ProductQueryDTO;
import com.pzhu.mall.modules.product.entity.Product;
import com.pzhu.mall.modules.product.entity.Sku;
import com.pzhu.mall.modules.product.service.ProductService;
import com.pzhu.mall.modules.shop.service.ShopService;
import com.pzhu.mall.modules.product.vo.ProductVO;
import com.pzhu.mall.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Objects;

/**
 * 商家端商品管理控制器。
 */
@Tag(name = "商家商品管理")
@RestController
@RequestMapping("/api/merchant/products")
@RequireRole(2)
public class MerchantProductController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MerchantProductController.class);

    @Resource
    private ProductService productService;

    @Resource
    private ShopService shopService;

    @Resource
    private org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;

    /**
     * L2-03 修复：库存变更后同步/失效 Redis 预扣 key。
     * <p>StockService 的库存 key（mall:stock:{skuId} / mall:stock:product:{productId}）
     * 由 DB 懒加载，此处删除 key 触发下次扣减时从 DB 重新加载，避免 Redis/DB 库存永久漂移。</p>
     */
    private void evictStockRedisKey(Long skuId, Long productId) {
        try {
            if (skuId != null) {
                stringRedisTemplate.delete(com.pzhu.mall.common.config.RedisKeyPrefix.STOCK + ":" + skuId);
            }
            if (productId != null) {
                stringRedisTemplate.delete(com.pzhu.mall.common.config.RedisKeyPrefix.STOCK + ":product:" + productId);
            }
        } catch (Exception e) {
            // DE_MIGHT_IGNORE 修复：记录 debug（Redis 不可用时忽略，下次扣减从 DB 懒加载兜底）
            log.debug("[商品] 失效 Redis 库存 key 失败 skuId={} productId={}", skuId, productId, e);
        }
    }

    @Operation(summary = "商家商品列表")
    @GetMapping
    public Result<PageResult<ProductVO>> list(ProductQueryDTO query) {
        Long merchantUserId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();
        Long shopId = shopService.getMerchantShopIdOrThrow(merchantUserId);

        query.setShopId(shopId);
        return Result.success(productService.listPage(query));
    }

    @Operation(summary = "商品详情")
    @GetMapping("/{id}")
    public Result<ProductVO> detail(@PathVariable Long id) {
        ProductVO product = productService.getDetail(id);
        // 校验商品归属当前商家店铺
        Long merchantUserId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();
        Long shopId = shopService.getMerchantShopIdOrThrow(merchantUserId);
        if (!Objects.equals(product.getShopId(), shopId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return Result.success(product);
    }

    @Operation(summary = "发布商品")
    @PostMapping
    public Result<ProductVO> create(@Validated @RequestBody CreateProductDTO dto) {
        Long merchantUserId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();
        Long shopId = shopService.getMerchantShopIdOrThrow(merchantUserId);

        Product product = new Product();
        product.setShopId(shopId);
        product.setCategoryId(dto.getCategoryId());
        product.setName(dto.getName());
        product.setMainImage(dto.getMainImage());
        product.setImages(dto.getImages());
        product.setDetail(dto.getDetail());
        product.setPrice(dto.getPrice());
        product.setOriginalPrice(dto.getOriginalPrice());
        product.setStock(dto.getStock());
        product.setStatus(ProductStatus.PENDING.getCode()); // 默认待审核
        product.setIsDeleted(0);

        // 构造 SKU 列表
        List<Sku> skuList = null;
        if (dto.getSkus() != null && !dto.getSkus().isEmpty()) {
            skuList = new ArrayList<>();
            for (SkuDTO skuDto : dto.getSkus()) {
                Sku sku = new Sku();
                sku.setSpecJson(skuDto.getSpecJson());
                sku.setPrice(skuDto.getPrice());
                sku.setStock(skuDto.getStock());
                sku.setImage(skuDto.getImage());
                skuList.add(sku);
            }
        }

        // 使用事务保护创建商品和 SKU
        product = productService.createWithSkus(product, skuList);

        return Result.success(productService.toVO(product));
    }

    @Operation(summary = "更新商品")
    @PutMapping("/{id}")
    @Transactional
    public Result<ProductVO> update(@PathVariable Long id, @Validated @RequestBody CreateProductDTO dto) {
        Product product = productService.getProductMapper().selectById(id);
        if (product == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        Long merchantUserId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();
        Long shopId = shopService.getMerchantShopIdOrThrow(merchantUserId);
        if (!Objects.equals(product.getShopId(), shopId)) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        product.setCategoryId(dto.getCategoryId());
        product.setName(dto.getName());
        product.setMainImage(dto.getMainImage());
        product.setImages(dto.getImages());
        product.setDetail(dto.getDetail());
        product.setPrice(dto.getPrice());
        product.setOriginalPrice(dto.getOriginalPrice());
        product.setStock(dto.getStock());
        if (ProductStatus.of(product.getStatus()) == ProductStatus.REJECTED) {
            product.setStatus(ProductStatus.PENDING.getCode());
        }
        productService.getProductMapper().updateById(product);

        // L2-03 修复：商品级库存变更后失效 Redis 预扣 key（懒加载兜底）
        evictStockRedisKey(null, product.getId());

        // 删除旧 SKU 并重新创建（简化处理）
        LambdaQueryWrapper<Sku> skuQw = new LambdaQueryWrapper<>();
        skuQw.eq(Sku::getProductId, id).eq(Sku::getIsDeleted, 0);
        List<Sku> oldSkus = productService.getSkuMapper().selectList(skuQw);
        for (Sku oldSku : oldSkus) {
            oldSku.setIsDeleted(1);
            productService.getSkuMapper().updateById(oldSku);
            // L2-03 修复：旧 SKU 库存 key 一并失效
            evictStockRedisKey(oldSku.getId(), null);
        }

        if (dto.getSkus() != null && !dto.getSkus().isEmpty()) {
            for (SkuDTO skuDto : dto.getSkus()) {
                Sku sku = new Sku();
                sku.setProductId(product.getId());
                sku.setSpecJson(skuDto.getSpecJson());
                sku.setPrice(skuDto.getPrice());
                sku.setStock(skuDto.getStock());
                sku.setImage(skuDto.getImage());
                sku.setIsDeleted(0);
                productService.getSkuMapper().insert(sku);
            }
        }

        return Result.success(productService.toVO(product));
    }

    @Operation(summary = "批量上下架/删除")
    @PutMapping("/batch")
    @Transactional
    public Result<Map<String, Object>> batchOperate(@Validated @RequestBody BatchOperateDTO dto) {
        Long merchantUserId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();
        Long shopId = shopService.getMerchantShopIdOrThrow(merchantUserId);

        List<String> failed = new ArrayList<>();
        int successCount = 0;
        for (Long productId : dto.getProductIds()) {
            Product product = productService.getProductMapper().selectById(productId);
            if (product == null || !Objects.equals(product.getShopId(), shopId)) {
                failed.add("商品 " + productId + " 不存在或不属于当前店铺");
                continue;
            }
            try {
                if ("on".equals(dto.getAction())) {
                    // H-02 修复：商家不得直接将商品置为 ONLINE（会绕过管理员审核）。
                    // "上架"仅表示提交审核：OFFLINE/REJECTED -> PENDING，最终上架由管理员审核完成。
                    ProductStatus st = ProductStatus.of(product.getStatus());
                    if (st == ProductStatus.ONLINE) {
                        failed.add("商品 " + productId + " 已上架，无需重复操作");
                        continue;
                    }
                    if (st == ProductStatus.PENDING) {
                        failed.add("商品 " + productId + " 已在待审核队列中");
                        continue;
                    }
                    product.setStatus(ProductStatus.PENDING.getCode());
                } else if ("off".equals(dto.getAction())) {
                    product.setStatus(ProductStatus.OFFLINE.getCode());
                } else if ("delete".equals(dto.getAction())) {
                    product.setIsDeleted(1);
                }
                productService.getProductMapper().updateById(product);
                successCount++;
            } catch (Exception e) {
                // P-05 修复：记录异常栈，避免批量操作失败根因不可查
                log.error("[商品] 批量操作失败 productId={} action={}", productId, dto.getAction(), e);
                failed.add("商品 " + productId + " 操作失败");
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", successCount);
        result.put("failed", failed);
        result.put("failedCount", failed.size());
        return Result.success(result);
    }

    // ---- DTOs ----

    public static class SkuDTO {
        private String specJson;
        // P-04 修复：SKU 价格/库存非负校验（此前可 null/负值入库）
        @javax.validation.constraints.NotNull(message = "SKU价格不能为空")
        @javax.validation.constraints.DecimalMin(value = "0", message = "SKU价格不能为负")
        private java.math.BigDecimal price;
        @javax.validation.constraints.NotNull(message = "SKU库存不能为空")
        @javax.validation.constraints.Min(value = 0, message = "SKU库存不能为负")
        private Integer stock;
        @javax.validation.constraints.Size(max = 500, message = "SKU图片URL最长500字符")
        private String image;

        public String getSpecJson() { return specJson; }
        public void setSpecJson(String specJson) { this.specJson = specJson; }
        public java.math.BigDecimal getPrice() { return price; }
        public void setPrice(java.math.BigDecimal price) { this.price = price; }
        public Integer getStock() { return stock; }
        public void setStock(Integer stock) { this.stock = stock; }
        public String getImage() { return image; }
        public void setImage(String image) { this.image = image; }
    }

    public static class CreateProductDTO {
        @NotNull
        private Long categoryId;
        @NotBlank
        @Size(max = 200, message = "商品名称最长200字符")
        private String name;
        @Size(max = 500, message = "主图URL最长500字符")
        private String mainImage;
        private String images;
        @Size(max = 20000, message = "商品详情最长20000字符")
        private String detail;
        @NotNull
        @javax.validation.constraints.DecimalMin(value = "0", message = "价格不能为负")
        private java.math.BigDecimal price;
        @javax.validation.constraints.DecimalMin(value = "0", message = "原价不能为负")
        private java.math.BigDecimal originalPrice;
        @NotNull
        @javax.validation.constraints.Min(value = 0, message = "库存不能为负")
        private Integer stock;
        private List<SkuDTO> skus;

        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getMainImage() { return mainImage; }
        public void setMainImage(String mainImage) { this.mainImage = mainImage; }
        public String getImages() { return images; }
        public void setImages(String images) { this.images = images; }
        public String getDetail() { return detail; }
        public void setDetail(String detail) { this.detail = detail; }
        public java.math.BigDecimal getPrice() { return price; }
        public void setPrice(java.math.BigDecimal price) { this.price = price; }
        public java.math.BigDecimal getOriginalPrice() { return originalPrice; }
        public void setOriginalPrice(java.math.BigDecimal originalPrice) { this.originalPrice = originalPrice; }
        public Integer getStock() { return stock; }
        public void setStock(Integer stock) { this.stock = stock; }
        public List<SkuDTO> getSkus() { return skus; }
        public void setSkus(List<SkuDTO> skus) { this.skus = skus; }
    }

    public static class BatchOperateDTO {
        @NotEmpty
        private List<Long> productIds;
        @NotBlank
        private String action; // on / off / delete

        public List<Long> getProductIds() { return productIds; }
        public void setProductIds(List<Long> productIds) { this.productIds = productIds; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
    }
}
