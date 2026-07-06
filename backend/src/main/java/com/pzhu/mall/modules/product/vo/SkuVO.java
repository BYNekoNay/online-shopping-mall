package com.pzhu.mall.modules.product.vo;

import java.math.BigDecimal;

/**
 * SKU 响应 VO。
 */
public class SkuVO {

    private Long id;
    private Long productId;
    private String specJson;
    private BigDecimal price;
    private Integer stock;
    private String image;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getSpecJson() { return specJson; }
    public void setSpecJson(String specJson) { this.specJson = specJson; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
}
