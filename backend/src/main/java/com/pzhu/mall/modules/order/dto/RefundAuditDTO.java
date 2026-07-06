package com.pzhu.mall.modules.order.dto;

/**
 * 退款审核 DTO。
 */
public class RefundAuditDTO {

    private Boolean approved;
    private String handleRemark;

    public Boolean getApproved() { return approved; }
    public void setApproved(Boolean approved) { this.approved = approved; }
    public String getHandleRemark() { return handleRemark; }
    public void setHandleRemark(String handleRemark) { this.handleRemark = handleRemark; }
}
