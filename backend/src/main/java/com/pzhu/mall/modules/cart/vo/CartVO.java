package com.pzhu.mall.modules.cart.vo;

import java.math.BigDecimal;

/**
 * 购物车项响应 VO。
 */
public class CartVO {

    private Long id;
    private Long productId;
    private Long skuId;
    private Integer quantity;
    private Integer selected;
    private String productName;
    private String mainImage;
    private BigDecimal price;
    private String specText;
    private Integer stock;
    private Boolean stockEnough;

    // FRONT-06 修复：补充店铺信息，供确认下单页跨店拆单展示（此前购物车项无 shopId/shopName，
    // 前端 Confirm.vue 全部商品合并到 undefined 组"店铺"，跨店拆单显示错误）
    private Long shopId;
    private String shopName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getSelected() { return selected; }
    public void setSelected(Integer selected) { this.selected = selected; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getMainImage() { return mainImage; }
    public void setMainImage(String mainImage) { this.mainImage = mainImage; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getSpecText() { return specText; }
    public void setSpecText(String specText) { this.specText = specText; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Boolean getStockEnough() { return stockEnough; }
    public void setStockEnough(Boolean stockEnough) { this.stockEnough = stockEnough; }
    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }
}
