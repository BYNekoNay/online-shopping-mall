package com.pzhu.mall.modules.order.vo;

import java.math.BigDecimal;

/**
 * 订单明细响应 VO。
 */
public class OrderItemVO {

    private Long id;
    private Long productId;
    private Long skuId;
    private String productName;
    private String productImage;
    private BigDecimal price;
    private Integer quantity;
    private Integer isGift;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getProductImage() { return productImage; }
    public void setProductImage(String productImage) { this.productImage = productImage; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getIsGift() { return isGift; }
    public void setIsGift(Integer isGift) { this.isGift = isGift; }
}
