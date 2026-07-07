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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 商家端商品管理控制器。
 */
@Tag(name = "商家商品管理")
@RestController
@RequestMapping("/merchant/products")
@RequireRole(2)
public class MerchantProductController {

    @Resource
    private ProductService productService;

    @Resource
    private ShopService shopService;

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
        if (!product.getShopId().equals(shopId)) {
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
        productService.getProductMapper().insert(product);

        // 保存 SKU 列表
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

    @Operation(summary = "更新商品")
    @PutMapping("/{id}")
    public Result<ProductVO> update(@PathVariable Long id, @Validated @RequestBody CreateProductDTO dto) {
        Product product = productService.getProductMapper().selectById(id);
        if (product == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        Long merchantUserId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();
        Long shopId = shopService.getMerchantShopIdOrThrow(merchantUserId);
        if (!product.getShopId().equals(shopId)) {
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

        // 删除旧 SKU 并重新创建（简化处理）
        LambdaQueryWrapper<Sku> skuQw = new LambdaQueryWrapper<>();
        skuQw.eq(Sku::getProductId, id).eq(Sku::getIsDeleted, 0);
        List<Sku> oldSkus = productService.getSkuMapper().selectList(skuQw);
        for (Sku oldSku : oldSkus) {
            oldSku.setIsDeleted(1);
            productService.getSkuMapper().updateById(oldSku);
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
    public Result<Void> batchOperate(@Validated @RequestBody BatchOperateDTO dto) {
        Long merchantUserId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();
        Long shopId = shopService.getMerchantShopIdOrThrow(merchantUserId);

        for (Long productId : dto.getProductIds()) {
            Product product = productService.getProductMapper().selectById(productId);
            if (product == null || !product.getShopId().equals(shopId)) {
                continue;
            }
            if ("on".equals(dto.getAction())) {
                product.setStatus(ProductStatus.ONLINE.getCode());
            } else if ("off".equals(dto.getAction())) {
                product.setStatus(ProductStatus.OFFLINE.getCode());
            } else if ("delete".equals(dto.getAction())) {
                product.setIsDeleted(1);
            }
            productService.getProductMapper().updateById(product);
        }

        return Result.success();
    }

    // ---- DTOs ----

    public static class SkuDTO {
        private String specJson;
        private java.math.BigDecimal price;
        private Integer stock;
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
        private Long categoryId;
        private String name;
        private String mainImage;
        private String images;
        private String detail;
        private java.math.BigDecimal price;
        private java.math.BigDecimal originalPrice;
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
        private List<Long> productIds;
        private String action; // on / off / delete

        public List<Long> getProductIds() { return productIds; }
        public void setProductIds(List<Long> productIds) { this.productIds = productIds; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
    }
}
