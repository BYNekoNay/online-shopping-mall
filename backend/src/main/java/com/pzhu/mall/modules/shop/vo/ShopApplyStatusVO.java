package com.pzhu.mall.modules.shop.vo;

/**
 * 商家入驻申请状态响应 VO。
 */
public class ShopApplyStatusVO {

    private boolean hasApplied;
    private Long shopId;
    private Integer status;
    private String rejectReason;

    public boolean isHasApplied() { return hasApplied; }
    public void setHasApplied(boolean hasApplied) { this.hasApplied = hasApplied; }
    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
}
