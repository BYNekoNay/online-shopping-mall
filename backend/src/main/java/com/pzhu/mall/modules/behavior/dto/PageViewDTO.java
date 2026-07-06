package com.pzhu.mall.modules.behavior.dto;

/**
 * 页面访问上报 DTO。
 */
public class PageViewDTO {

    private Long userId;
    private String sessionId;
    private String pagePath;
    private String referrerPage;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getPagePath() { return pagePath; }
    public void setPagePath(String pagePath) { this.pagePath = pagePath; }
    public String getReferrerPage() { return referrerPage; }
    public void setReferrerPage(String referrerPage) { this.referrerPage = referrerPage; }
}
