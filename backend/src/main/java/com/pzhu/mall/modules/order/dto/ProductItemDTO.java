package com.pzhu.mall.modules.order.dto;

/**
 * 下单商品项 DTO。
 */
public class ProductItemDTO {

    /**
     * 商品 ID。
     */
    private Long productId;

    /**
     * SKU ID（可选，无规格商品可为空）。
     */
    private Long skuId;

    /**
     * 购买数量。
     */
    private Integer quantity;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
