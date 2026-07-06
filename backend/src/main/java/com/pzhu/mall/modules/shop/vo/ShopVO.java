package com.pzhu.mall.modules.shop.vo;

/**
 * 店铺信息响应 VO。
 */
public class ShopVO {

    private Long id;
    private Long merchantUserId;
    private String name;
    private String logo;
    private String description;
    private String decorationConfig;
    private Integer level;
    private Integer status;
    private String contactName;
    private String contactPhone;
    private String licenseNo;
    private String licenseImage;
    private String applyReason;
    private String rejectReason;
    private java.time.LocalDateTime createTime;
    private java.time.LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMerchantUserId() { return merchantUserId; }
    public void setMerchantUserId(Long merchantUserId) { this.merchantUserId = merchantUserId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDecorationConfig() { return decorationConfig; }
    public void setDecorationConfig(String decorationConfig) { this.decorationConfig = decorationConfig; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getLicenseNo() { return licenseNo; }
    public void setLicenseNo(String licenseNo) { this.licenseNo = licenseNo; }
    public String getLicenseImage() { return licenseImage; }
    public void setLicenseImage(String licenseImage) { this.licenseImage = licenseImage; }
    public String getApplyReason() { return applyReason; }
    public void setApplyReason(String applyReason) { this.applyReason = applyReason; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public java.time.LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(java.time.LocalDateTime createTime) { this.createTime = createTime; }
    public java.time.LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(java.time.LocalDateTime updateTime) { this.updateTime = updateTime; }
}
