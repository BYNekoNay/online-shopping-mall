package com.pzhu.mall.modules.product.vo;

/**
 * 分类响应 VO。
 */
public class CategoryVO {

    private Long id;
    private Long parentId;
    private String name;
    private String icon;
    private Integer sort;
    private Integer status;
    private java.util.List<CategoryVO> children;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public java.util.List<CategoryVO> getChildren() { return children; }
    public void setChildren(java.util.List<CategoryVO> children) { this.children = children; }
}
