package com.pzhu.mall.modules.order.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单明细实体（对应 order_item 表）。
 */
public class OrderItem {

    private Long id;
    private Long orderId;
    private Long productId;
    private Long skuId;
    private String productNameSnapshot;
    private String productImageSnapshot;
    private BigDecimal price;
    private Integer quantity;
    private Integer isGift;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public String getProductNameSnapshot() { return productNameSnapshot; }
    public void setProductNameSnapshot(String productNameSnapshot) { this.productNameSnapshot = productNameSnapshot; }
    public String getProductImageSnapshot() { return productImageSnapshot; }
    public void setProductImageSnapshot(String productImageSnapshot) { this.productImageSnapshot = productImageSnapshot; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getIsGift() { return isGift; }
    public void setIsGift(Integer isGift) { this.isGift = isGift; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
