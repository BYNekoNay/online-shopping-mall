package com.pzhu.mall.modules.shop.dto;

/**
 * 商家入驻申请 DTO。
 * <p>SH-01 修复：字段级校验注解，禁止空值/超长入库。</p>
 */
public class ShopApplyDTO {

    @javax.validation.constraints.NotBlank(message = "店铺名称不能为空")
    @javax.validation.constraints.Size(max = 100, message = "店铺名称最长100字符")
    private String name;

    @javax.validation.constraints.NotBlank(message = "联系人不能为空")
    @javax.validation.constraints.Size(max = 50, message = "联系人最长50字符")
    private String contactName;

    @javax.validation.constraints.NotBlank(message = "联系电话不能为空")
    @javax.validation.constraints.Pattern(regexp = "^1[3-9]\\d{9}$", message = "联系电话格式不正确")
    private String contactPhone;

    @javax.validation.constraints.NotBlank(message = "营业执照号不能为空")
    @javax.validation.constraints.Size(max = 50, message = "营业执照号最长50字符")
    private String licenseNo;

    @javax.validation.constraints.Size(max = 500, message = "营业执照图片URL最长500字符")
    private String licenseImage;

    @javax.validation.constraints.Size(max = 500, message = "申请理由最长500字符")
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
