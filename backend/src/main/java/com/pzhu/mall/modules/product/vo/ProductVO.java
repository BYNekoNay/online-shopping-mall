package com.pzhu.mall.modules.product.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品响应 VO。
 */
public class ProductVO {

    private Long id;
    private Long shopId;
    private Long categoryId;
    private String name;
    private String mainImage;
    private String images;
    private String detail;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stock;
    private Integer sales;
    private Integer status;
    private String categoryName;
    private String shopName;
    private List<SkuVO> skuList;
    private java.time.LocalDateTime createTime;
    /** 当前商品命中的生效促销（无命中时为 null） */
    private Object activePromotion;

    /** FRONT-QA-02 修复：真实商品平均评分（review 表动态聚合，无评价为 null） */
    private Double rating;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
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
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Integer getSales() { return sales; }
    public void setSales(Integer sales) { this.sales = sales; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }
    public List<SkuVO> getSkuList() { return skuList; }
    public void setSkuList(List<SkuVO> skuList) { this.skuList = skuList; }
    public java.time.LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(java.time.LocalDateTime createTime) { this.createTime = createTime; }
    public Object getActivePromotion() { return activePromotion; }
    public void setActivePromotion(Object activePromotion) { this.activePromotion = activePromotion; }
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
}
