package com.pzhu.mall.modules.behavior.dto;

/**
 * 行为记录 DTO。
 */
public class BehaviorRecordDTO {

    private Long userId;
    private Long productId;
    private Integer behaviorType;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Integer getBehaviorType() { return behaviorType; }
    public void setBehaviorType(Integer behaviorType) { this.behaviorType = behaviorType; }
}
