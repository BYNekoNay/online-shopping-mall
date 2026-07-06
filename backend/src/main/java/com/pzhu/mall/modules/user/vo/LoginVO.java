package com.pzhu.mall.modules.user.vo;

/**
 * 登录响应 VO。
 */
public class LoginVO {

    private String token;
    private Long userId;
    private Integer role;
    private String nickname;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getRole() { return role; }
    public void setRole(Integer role) { this.role = role; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
}
