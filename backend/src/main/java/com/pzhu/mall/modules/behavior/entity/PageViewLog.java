package com.pzhu.mall.modules.behavior.entity;

import java.time.LocalDateTime;

/**
 * 页面访问日志实体（对应 page_view_log 表）。
 */
public class PageViewLog {

    private Long id;
    private Long userId;
    private String sessionId;
    private String pagePath;
    private String referrerPage;
    private LocalDateTime enterTime;
    private LocalDateTime leaveTime;
    private Integer stayDuration;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getPagePath() { return pagePath; }
    public void setPagePath(String pagePath) { this.pagePath = pagePath; }
    public String getReferrerPage() { return referrerPage; }
    public void setReferrerPage(String referrerPage) { this.referrerPage = referrerPage; }
    public LocalDateTime getEnterTime() { return enterTime; }
    public void setEnterTime(LocalDateTime enterTime) { this.enterTime = enterTime; }
    public LocalDateTime getLeaveTime() { return leaveTime; }
    public void setLeaveTime(LocalDateTime leaveTime) { this.leaveTime = leaveTime; }
    public Integer getStayDuration() { return stayDuration; }
    public void setStayDuration(Integer stayDuration) { this.stayDuration = stayDuration; }
}
