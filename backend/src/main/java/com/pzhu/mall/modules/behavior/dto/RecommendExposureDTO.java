package com.pzhu.mall.modules.behavior.dto;

import javax.validation.constraints.NotNull;

/**
 * 推荐位曝光埋点 DTO。
 */
public class RecommendExposureDTO {

    /** 用户ID，未登录可为 null */
    private Long userId;

    /** 曝光位置：home-guess / product-similar */
    @NotNull(message = "source 不能为空")
    private String source;

    /** 当前曝光商品列表（productId 数组，用于后续 CTR 归因） */
    @NotNull(message = "productIds 不能为空")
    private java.util.List<Long> productIds;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public java.util.List<Long> getProductIds() { return productIds; }
    public void setProductIds(java.util.List<Long> productIds) { this.productIds = productIds; }
}
