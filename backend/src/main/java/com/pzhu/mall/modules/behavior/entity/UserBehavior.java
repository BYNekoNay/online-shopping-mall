package com.pzhu.mall.modules.behavior.entity;

import java.time.LocalDateTime;

/**
 * 用户行为实体（对应 user_behavior 表）。
 */
public class UserBehavior {

    private Long id;
    private Long userId;
    private Long productId;
    private Integer behaviorType;
    private java.math.BigDecimal behaviorWeight;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Integer getBehaviorType() { return behaviorType; }
    public void setBehaviorType(Integer behaviorType) { this.behaviorType = behaviorType; }
    public java.math.BigDecimal getBehaviorWeight() { return behaviorWeight; }
    public void setBehaviorWeight(java.math.BigDecimal behaviorWeight) { this.behaviorWeight = behaviorWeight; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
