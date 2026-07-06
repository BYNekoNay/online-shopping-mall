package com.pzhu.mall.modules.statistics.entity;

import java.time.LocalDateTime;

/**
 * 搜索历史实体（对应 search_history 表）。
 */
public class SearchHistory {

    private Long id;
    private Long userId;
    private String keyword;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
