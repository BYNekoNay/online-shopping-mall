package com.pzhu.mall.modules.admin.controller;

import com.pzhu.mall.common.result.Result;
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
    public Result<Long> create(@RequestBody Category category) {
        categoryMapper.insert(category);
        return Result.success(category.getId());
    }

    @Operation(summary = "更新分类")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Category category) {
        category.setId(id);
        categoryMapper.updateById(category);
        return Result.success();
    }
}
