package com.pzhu.mall.modules.recommend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 推荐结果实体（对应 recommend_result 表）。
 */
public class RecommendResult {

    private Long id;
    private Long userId;
    private Long productId;
    private Integer algorithmType;
    private BigDecimal score;
    private LocalDateTime generateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Integer getAlgorithmType() { return algorithmType; }
    public void setAlgorithmType(Integer algorithmType) { this.algorithmType = algorithmType; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
    public LocalDateTime getGenerateTime() { return generateTime; }
    public void setGenerateTime(LocalDateTime generateTime) { this.generateTime = generateTime; }
}
