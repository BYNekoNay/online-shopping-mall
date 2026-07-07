package com.pzhu.mall.modules.marketing.vo;

import com.pzhu.mall.modules.marketing.entity.Coupon;

import java.time.LocalDateTime;

/**
 * 用户优惠券（含模板信息）VO。
 */
public class UserCouponVO {

    private Long id;          // user_coupon.id
    private Long couponId;
    private Integer status;
    private LocalDateTime useTime;
    private Long relatedOrderId;
    private LocalDateTime createTime;

    // 优惠券模板信息
    private String name;
    private String type;
    private String discountRule;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDiscountRule() { return discountRule; }
    public void setDiscountRule(String discountRule) { this.discountRule = discountRule; }
    public LocalDateTime getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDateTime validFrom) { this.validFrom = validFrom; }
    public LocalDateTime getValidTo() { return validTo; }
    public void setValidTo(LocalDateTime validTo) { this.validTo = validTo; }
}
