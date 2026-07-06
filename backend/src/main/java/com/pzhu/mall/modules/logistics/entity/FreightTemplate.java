package com.pzhu.mall.modules.logistics.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 运费模板实体（对应 freight_template 表）。
 */
public class FreightTemplate {

    private Long id;
    private Long shopId;
    private String name;
    private String regionRuleJson;
    private BigDecimal freeShippingThreshold;
    private BigDecimal defaultFee;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRegionRuleJson() { return regionRuleJson; }
    public void setRegionRuleJson(String regionRuleJson) { this.regionRuleJson = regionRuleJson; }
    public BigDecimal getFreeShippingThreshold() { return freeShippingThreshold; }
    public void setFreeShippingThreshold(BigDecimal freeShippingThreshold) { this.freeShippingThreshold = freeShippingThreshold; }
    public BigDecimal getDefaultFee() { return defaultFee; }
    public void setDefaultFee(BigDecimal defaultFee) { this.defaultFee = defaultFee; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
}
