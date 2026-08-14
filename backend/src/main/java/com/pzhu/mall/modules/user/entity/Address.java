package com.pzhu.mall.modules.user.entity;

import java.time.LocalDateTime;

/**
 * 收货地址实体（对应 address 表）。
 * <p>U-03 修复：字段级校验注解（作为 @RequestBody 校验载体），
 * 使 UserController.addAddress/updateAddress 的 @Validated 真正生效。</p>
 */
public class Address {

    private Long id;
    private Long userId;

    @javax.validation.constraints.NotBlank(message = "收货人不能为空")
    @javax.validation.constraints.Size(max = 50, message = "收货人最长50字符")
    private String receiver;

    @javax.validation.constraints.NotBlank(message = "手机号不能为空")
    @javax.validation.constraints.Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @javax.validation.constraints.NotBlank(message = "省份不能为空")
    private String province;

    @javax.validation.constraints.NotBlank(message = "城市不能为空")
    private String city;

    private String district;

    @javax.validation.constraints.NotBlank(message = "详细地址不能为空")
    @javax.validation.constraints.Size(max = 200, message = "详细地址最长200字符")
    private String detail;

    private Integer isDefault;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public Integer getIsDefault() { return isDefault; }
    public void setIsDefault(Integer isDefault) { this.isDefault = isDefault; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
