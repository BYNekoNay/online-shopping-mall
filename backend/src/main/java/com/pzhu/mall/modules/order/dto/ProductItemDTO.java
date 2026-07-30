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

    /**
     * 来源购物车项 ID（H-3/M-8 修复）。
     * <p>仅在从购物车下单时由服务端回填，用于按成功分组精确清理购物车；
     * 直接下单（请求体携带 productItems）时为空。不参与前端入参。
     */
    private Long cartId;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Long getCartId() { return cartId; }
    public void setCartId(Long cartId) { this.cartId = cartId; }
}
