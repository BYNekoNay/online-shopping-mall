package com.pzhu.mall.modules.admin.vo;

/**
 * 管理员可见的用户信息（不含密码哈希）。
 */
public class AdminUserVO {
    private Long id;
    private String username;
    private String nickname;
    private Integer role;
    private Integer status;
    private String phone;
    private String email;
    private java.time.LocalDateTime createTime;

    public AdminUserVO() {}

    public AdminUserVO(Long id, String username, String nickname, Integer role, Integer status,
                       String phone, String email, java.time.LocalDateTime createTime) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.role = role;
        this.status = status;
        this.phone = phone;
        this.email = email;
        this.createTime = createTime;
    }

    public static AdminUserVO from(com.pzhu.mall.modules.user.entity.User user) {
        return new AdminUserVO(
                user.getId(), user.getUsername(), user.getNickname(),
                user.getRole(), user.getStatus(),
                user.getPhone(), user.getEmail(), user.getCreateTime()
        );
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public Integer getRole() { return role; }
    public void setRole(Integer role) { this.role = role; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public java.time.LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(java.time.LocalDateTime createTime) { this.createTime = createTime; }
}
