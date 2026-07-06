package com.pzhu.mall.modules.marketing.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券模板实体（对应 coupon 表）。
 */
public class Coupon {

    private Long id;
    private String name;
    private Integer type;
    private Long shopId;
    private String discountRule;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private Integer stock;
    private Integer receivedCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }
    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public String getDiscountRule() { return discountRule; }
    public void setDiscountRule(String discountRule) { this.discountRule = discountRule; }
    public LocalDateTime getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDateTime validFrom) { this.validFrom = validFrom; }
    public LocalDateTime getValidTo() { return validTo; }
    public void setValidTo(LocalDateTime validTo) { this.validTo = validTo; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Integer getReceivedCount() { return receivedCount; }
    public void setReceivedCount(Integer receivedCount) { this.receivedCount = receivedCount; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
}
