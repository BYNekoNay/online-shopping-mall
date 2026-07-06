package com.pzhu.mall.modules.shop.dto;

/**
 * 更新店铺信息 DTO。
 */
public class ShopUpdateDTO {

    private String name;
    private String logo;
    private String description;
    private String decorationConfig;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDecorationConfig() { return decorationConfig; }
    public void setDecorationConfig(String decorationConfig) { this.decorationConfig = decorationConfig; }
}
