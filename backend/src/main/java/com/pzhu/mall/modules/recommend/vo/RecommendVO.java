package com.pzhu.mall.modules.recommend.vo;

import java.math.BigDecimal;

/**
 * 推荐结果响应 VO（包含商品基础信息 + 推荐分数与算法类型）。
 *
 * <p>字段对齐 {@code 09-接口规范.md §2.2} 的响应格式：
 * {@code { "productId","name","mainImage","price","score","algorithmType" }}。
 */
public class RecommendVO {

    private Long productId;
    private String name;
    private String mainImage;
    private BigDecimal price;
    private Double score;
    private Integer algorithmType;

    /** FRONT-QA-02 修复：真实商品平均评分（review 表聚合，无评价为 null，前端展示星级用） */
    private Double rating;

    /** FRONT-QA-02 修复：真实商品销量（product.sales） */
    private Integer sales;

    public static RecommendVO from(Object product, double score, int algorithmType) {
        RecommendVO vo = new RecommendVO();
        if (product instanceof com.pzhu.mall.modules.product.entity.Product p) {
            vo.setProductId(p.getId());
            vo.setName(p.getName());
            vo.setMainImage(p.getMainImage());
            vo.setPrice(p.getPrice());
            vo.setSales(p.getSales());
        }
        vo.setScore(score);
        vo.setAlgorithmType(algorithmType);
        return vo;
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getMainImage() { return mainImage; }
    public void setMainImage(String mainImage) { this.mainImage = mainImage; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
    public Integer getAlgorithmType() { return algorithmType; }
    public void setAlgorithmType(Integer algorithmType) { this.algorithmType = algorithmType; }
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
    public Integer getSales() { return sales; }
    public void setSales(Integer sales) { this.sales = sales; }
}
