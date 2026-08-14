package com.pzhu.mall.modules.order.dto;

/**
 * 退款审核 DTO。
 */
public class RefundAuditDTO {

    // O-14 修复：approved 必填（此前可空，null 时按"拒绝"处理语义含糊）
    @javax.validation.constraints.NotNull(message = "approved 不能为空")
    private Boolean approved;
    private String handleRemark;

    public Boolean getApproved() { return approved; }
    public void setApproved(Boolean approved) { this.approved = approved; }
    public String getHandleRemark() { return handleRemark; }
    public void setHandleRemark(String handleRemark) { this.handleRemark = handleRemark; }
}
