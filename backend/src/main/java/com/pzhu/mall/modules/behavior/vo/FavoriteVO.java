package com.pzhu.mall.modules.behavior.vo;

import java.math.BigDecimal;

/**
 * 收藏列表轻量 VO（F-04 修复）。
 * <p>原实现直接返回 Product 实体（含 detail 富文本等大字段），列表响应臃肿；
 * 改为仅返回收藏展示所需字段。</p>
 */
public class FavoriteVO {

    /** 商品 ID（与 Product.id 同名，兼容前端 item.id 用法） */
    private Long id;
    private Long productId;
    private String name;
    private String mainImage;
    private BigDecimal price;
    private Integer sales;

    public static FavoriteVO from(com.pzhu.mall.modules.product.entity.Product p) {
        FavoriteVO vo = new FavoriteVO();
        vo.setId(p.getId());
        vo.setProductId(p.getId());
        vo.setName(p.getName());
        vo.setMainImage(p.getMainImage());
        vo.setPrice(p.getPrice());
        vo.setSales(p.getSales());
        return vo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getMainImage() { return mainImage; }
    public void setMainImage(String mainImage) { this.mainImage = mainImage; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getSales() { return sales; }
    public void setSales(Integer sales) { this.sales = sales; }
}
