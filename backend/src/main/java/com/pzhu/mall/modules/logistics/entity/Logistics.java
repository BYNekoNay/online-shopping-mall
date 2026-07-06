package com.pzhu.mall.modules.logistics.entity;

import java.time.LocalDateTime;

/**
 * 物流信息实体（对应 logistics 表）。
 */
public class Logistics {

    private Long id;
    private Long orderId;
    private String company;
    private String companyCode;
    private String trackingNo;
    private Integer status;
    private String lastTrackInfo;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getCompanyCode() { return companyCode; }
    public void setCompanyCode(String companyCode) { this.companyCode = companyCode; }
    public String getTrackingNo() { return trackingNo; }
    public void setTrackingNo(String trackingNo) { this.trackingNo = trackingNo; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getLastTrackInfo() { return lastTrackInfo; }
    public void setLastTrackInfo(String lastTrackInfo) { this.lastTrackInfo = lastTrackInfo; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
