package com.pzhu.mall.modules.user.vo;

/**
 * 用户信息响应 VO（过滤敏感字段）。
 */
public class UserProfileVO {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String phone;
    private String email;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
