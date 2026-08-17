package com.pzhu.mall.modules.product.vo;

import java.time.LocalDateTime;

/**
 * 商品评价响应 VO（FRONT-04 修复）。
 * <p>在 Review 实体基础上补充 userNickname（评价人昵称），
 * 供前端商品详情评价列表展示真实昵称（此前直接返回 Review 实体，
 * 前端 review.userNickname 恒为 undefined，全部显示"匿名用户"）。</p>
 */
public class ReviewVO {

    private Long id;
    private Long orderItemId;
    private Long userId;
    private Long productId;
    private Integer rating;
    private String content;
    private String images;
    private LocalDateTime createTime;

    /** 评价人昵称（联表 user 表填充，无则 null，前端回退"匿名用户"） */
    private String userNickname;

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
    public String getUserNickname() { return userNickname; }
    public void setUserNickname(String userNickname) { this.userNickname = userNickname; }
}
