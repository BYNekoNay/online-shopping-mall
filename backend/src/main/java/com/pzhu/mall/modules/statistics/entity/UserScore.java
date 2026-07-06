package com.pzhu.mall.modules.statistics.entity;

import java.time.LocalDateTime;

/**
 * 用户评分矩阵实体（对应 user_score 表）。
 */
public class UserScore {

    private Long id;
    private Long userId;
    private Long productId;
    private java.math.BigDecimal score;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public java.math.BigDecimal getScore() { return score; }
    public void setScore(java.math.BigDecimal score) { this.score = score; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
