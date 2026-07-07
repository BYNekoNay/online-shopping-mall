package com.pzhu.mall.modules.statistics.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 商家销售趋势项。
 */
public class SalesTrendVO {

    private String date;
    private BigDecimal amount;
    private Integer orders;

    public SalesTrendVO() {}

    public SalesTrendVO(String date, BigDecimal amount, Integer orders) {
        this.date = date;
        this.amount = amount;
        this.orders = orders;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Integer getOrders() { return orders; }
    public void setOrders(Integer orders) { this.orders = orders; }
}
