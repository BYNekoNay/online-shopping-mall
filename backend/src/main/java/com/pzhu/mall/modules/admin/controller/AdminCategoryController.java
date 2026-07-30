package com.pzhu.mall.modules.admin.controller;

import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.modules.product.entity.Category;
import com.pzhu.mall.modules.product.mapper.CategoryMapper;
import com.pzhu.mall.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 管理员端分类管理控制器。
 */
@Tag(name = "管理员-分类管理")
@RestController
@RequestMapping("/api/admin/categories")
@RequireRole(3)
public class AdminCategoryController {

    @Resource
    private CategoryMapper categoryMapper;

    @Operation(summary = "分类列表")
    @GetMapping
    public Result<List<Category>> list() {
        var qw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Category>();
        qw.eq(Category::getIsDeleted, 0);
        return Result.success(categoryMapper.selectList(qw));
    }

    @Operation(summary = "新增分类")
    @PostMapping
    public Result<Long> create(@RequestBody CategoryDTO dto) {
        // H-04 修复：使用 DTO 接收，禁止注入 id/isDeleted/createTime 等服务端字段（Mass Assignment）
        Category category = new Category();
        category.setParentId(dto.getParentId());
        category.setName(dto.getName());
        category.setIcon(dto.getIcon());
        category.setSort(dto.getSort());
        category.setStatus(dto.getStatus());
        category.setIsDeleted(0);
        categoryMapper.insert(category);
        return Result.success(category.getId());
    }

    @Operation(summary = "更新分类")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody CategoryDTO dto) {
        Category category = categoryMapper.selectById(id);
        if (category == null || Integer.valueOf(1).equals(category.getIsDeleted())) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        // H-04 修复：仅覆盖允许编辑的字段，不触碰 id/isDeleted/createTime
        if (dto.getParentId() != null) {
            category.setParentId(dto.getParentId());
        }
        if (dto.getName() != null) {
            category.setName(dto.getName());
        }
        if (dto.getIcon() != null) {
            category.setIcon(dto.getIcon());
        }
        if (dto.getSort() != null) {
            category.setSort(dto.getSort());
        }
        if (dto.getStatus() != null) {
            category.setStatus(dto.getStatus());
        }
        categoryMapper.updateById(category);
        return Result.success();
    }

    /** 分类编辑 DTO（仅包含允许客户端控制的字段）。 */
    public static class CategoryDTO {
        private Long parentId;
        private String name;
        private String icon;
        private Integer sort;
        private Integer status;

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
    }
}
