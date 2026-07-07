package com.pzhu.mall.modules.order.vo;

/**
 * 支付响应 VO。
 */
public class PayVO {

    private Boolean paySuccess;
    private String payNo;

    public Boolean getPaySuccess() { return paySuccess; }
    public void setPaySuccess(Boolean paySuccess) { this.paySuccess = paySuccess; }
    public String getPayNo() { return payNo; }
    public void setPayNo(String payNo) { this.payNo = payNo; }
}
