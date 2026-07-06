package com.pzhu.mall.modules.order.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付记录实体（对应 payment 表）。
 */
public class Payment {

    private Long id;
    private Long orderId;
    private String payNo;
    private BigDecimal amount;
    private Integer payType;
    private Integer status;
    private LocalDateTime callbackTime;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getPayNo() { return payNo; }
    public void setPayNo(String payNo) { this.payNo = payNo; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Integer getPayType() { return payType; }
    public void setPayType(Integer payType) { this.payType = payType; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCallbackTime() { return callbackTime; }
    public void setCallbackTime(LocalDateTime callbackTime) { this.callbackTime = callbackTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
