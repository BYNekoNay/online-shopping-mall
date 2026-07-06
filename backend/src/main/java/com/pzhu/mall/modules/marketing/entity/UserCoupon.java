package com.pzhu.mall.modules.marketing.entity;

import java.time.LocalDateTime;

/**
 * 用户领取的优惠券实体（对应 user_coupon 表）。
 */
public class UserCoupon {

    private Long id;
    private Long userId;
    private Long couponId;
    private Integer status;
    private LocalDateTime useTime;
    private Long relatedOrderId;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getCouponId() { return couponId; }
    public void setCouponId(Long couponId) { this.couponId = couponId; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getUseTime() { return useTime; }
    public void setUseTime(LocalDateTime useTime) { this.useTime = useTime; }
    public Long getRelatedOrderId() { return relatedOrderId; }
    public void setRelatedOrderId(Long relatedOrderId) { this.relatedOrderId = relatedOrderId; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
