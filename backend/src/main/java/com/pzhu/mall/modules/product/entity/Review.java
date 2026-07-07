package com.pzhu.mall.modules.product.entity;

import java.time.LocalDateTime;

/**
 * 商品评价实体（对应 review 表）。
 */
public class Review {

    private Long id;
    private Long orderItemId;
    private Long userId;
    private Long productId;
    private Integer rating;
    private String content;
    private String images;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderItemId() { return orderItemId; }
    public void setOrderItemId(Long orderItemId) { this.orderItemId = orderItemId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getImages() { return images; }
    public void setImages(String images) { this.images = images; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
