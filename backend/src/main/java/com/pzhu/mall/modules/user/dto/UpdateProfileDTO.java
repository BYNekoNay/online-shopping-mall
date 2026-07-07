package com.pzhu.mall.modules.user.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 更新个人信息请求 DTO。
 */
public class UpdateProfileDTO {

    @Size(max = 50, message = "Nickname max 50 characters")
    private String nickname;

    @Size(max = 255, message = "Avatar URL too long")
    private String avatar;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "Phone format invalid")
    private String phone;

    @Email(message = "Email format invalid")
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
