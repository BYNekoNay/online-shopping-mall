package com.pzhu.mall.modules.behavior.dto;

import javax.validation.constraints.NotNull;

/**
 * 推荐位点击埋点 DTO。
 */
public class RecommendClickDTO {

    /** 用户ID，未登录可为 null */
    private Long userId;

    /** 点击来源：home-guess / product-similar */
    @NotNull(message = "source 不能为空")
    private String source;

    /** 被点击商品 ID */
    @NotNull(message = "productId 不能为空")
    private Long productId;

    /** 曝光时该商品在列表中的位置（1-based，用于计算 CTR 分位） */
    private Integer position;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }
}
