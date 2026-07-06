package com.pzhu.mall.modules.user.dto;

/**
 * 更新个人信息请求 DTO。
 */
public class UpdateProfileDTO {

    private String nickname;
    private String avatar;
    private String phone;
    private String email;

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
