package com.pzhu.mall.modules.statistics.vo;

import java.math.BigDecimal;

/**
 * 热销商品项。
 */
public class TopProductVO {

    private Long productId;
    private String name;
    private Integer sales;
    private BigDecimal amount;

    public TopProductVO() {}

    public TopProductVO(Long productId, String name, Integer sales, BigDecimal amount) {
        this.productId = productId;
        this.name = name;
        this.sales = sales;
        this.amount = amount;
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getSales() { return sales; }
    public void setSales(Integer sales) { this.sales = sales; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
