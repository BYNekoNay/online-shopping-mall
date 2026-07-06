package com.pzhu.mall.modules.order.dto;

import java.math.BigDecimal;

/**
 * 退款申请 DTO。
 */
public class RefundApplyDTO {

    private Long orderId;
    private Long orderItemId;
    private Integer type;
    private String reason;
    private BigDecimal amount;

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getOrderItemId() { return orderItemId; }
    public void setOrderItemId(Long orderItemId) { this.orderItemId = orderItemId; }
    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
