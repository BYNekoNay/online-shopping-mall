package com.pzhu.mall.modules.shop.dto;

/**
 * 商家入驻申请 DTO。
 */
public class ShopApplyDTO {

    private String name;
    private String contactName;
    private String contactPhone;
    private String licenseNo;
    private String licenseImage;
    private String applyReason;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
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
}
