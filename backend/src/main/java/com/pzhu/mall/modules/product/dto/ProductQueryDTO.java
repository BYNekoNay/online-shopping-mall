package com.pzhu.mall.modules.product.dto;

/**
 * 商品搜索/列表查询参数 DTO。
 */
public class ProductQueryDTO {

    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private Long categoryId;
    private Long shopId;
    private String keyword;
    private Integer status;
    private java.math.BigDecimal minPrice;
    private java.math.BigDecimal maxPrice;

    public Integer getPageNum() { return pageNum; }
    public void setPageNum(Integer pageNum) { this.pageNum = pageNum; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public java.math.BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(java.math.BigDecimal minPrice) { this.minPrice = minPrice; }
    public java.math.BigDecimal getMaxPrice() { return maxPrice; }
    public void setMaxPrice(java.math.BigDecimal maxPrice) { this.maxPrice = maxPrice; }
}
