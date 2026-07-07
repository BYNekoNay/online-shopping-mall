package com.pzhu.mall.modules.order.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单估价响应（下单确认页用）。
 */
public class OrderEstimateVO {

    private Long shopId;
    private String shopName;
    private BigDecimal goodsAmount;
    private BigDecimal freightAmount;
    private BigDecimal promotionDiscountAmount;
    private BigDecimal couponDiscountAmount;
    private BigDecimal pointsDeductAmount;
    private BigDecimal discountAmount;
    private BigDecimal payAmount;
    private List<ItemEstimateVO> items;

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }
    public BigDecimal getGoodsAmount() { return goodsAmount; }
    public void setGoodsAmount(BigDecimal goodsAmount) { this.goodsAmount = goodsAmount; }
    public BigDecimal getFreightAmount() { return freightAmount; }
    public void setFreightAmount(BigDecimal freightAmount) { this.freightAmount = freightAmount; }
    public BigDecimal getPromotionDiscountAmount() { return promotionDiscountAmount; }
    public void setPromotionDiscountAmount(BigDecimal promotionDiscountAmount) { this.promotionDiscountAmount = promotionDiscountAmount; }
    public BigDecimal getCouponDiscountAmount() { return couponDiscountAmount; }
    public void setCouponDiscountAmount(BigDecimal couponDiscountAmount) { this.couponDiscountAmount = couponDiscountAmount; }
    public BigDecimal getPointsDeductAmount() { return pointsDeductAmount; }
    public void setPointsDeductAmount(BigDecimal pointsDeductAmount) { this.pointsDeductAmount = pointsDeductAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public BigDecimal getPayAmount() { return payAmount; }
    public void setPayAmount(BigDecimal payAmount) { this.payAmount = payAmount; }
    public List<ItemEstimateVO> getItems() { return items; }
    public void setItems(List<ItemEstimateVO> items) { this.items = items; }

    public static class ItemEstimateVO {
        private Long productId;
        private String productName;
        private String productImage;
        private BigDecimal price;
        private Integer quantity;

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public String getProductImage() { return productImage; }
        public void setProductImage(String productImage) { this.productImage = productImage; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
}
