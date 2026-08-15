package com.pzhu.mall.modules.marketing.entity;

import java.time.LocalDateTime;

/**
 * 积分记录实体（对应 points_record 表）。
 */
public class PointsRecord {

    private Long id;
    private Long userId;
    private Integer changeAmount;
    private Integer type;
    private Long relatedOrderId;
    // C-2：积分过期时间（NULL=永不过期）
    private LocalDateTime expireTime;
    // C-2：是否已过期清理（防重复扣减）
    private Integer expired;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getChangeAmount() { return changeAmount; }
    public void setChangeAmount(Integer changeAmount) { this.changeAmount = changeAmount; }
    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }
    public Long getRelatedOrderId() { return relatedOrderId; }
    public void setRelatedOrderId(Long relatedOrderId) { this.relatedOrderId = relatedOrderId; }
    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }
    public Integer getExpired() { return expired; }
    public void setExpired(Integer expired) { this.expired = expired; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
