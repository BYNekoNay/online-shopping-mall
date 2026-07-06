package com.pzhu.mall.modules.product.controller;

import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.modules.product.entity.Category;
import com.pzhu.mall.modules.product.service.CategoryService;
import com.pzhu.mall.modules.product.vo.CategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 商品分类控制器。
 */
@Tag(name = "商品分类")
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Resource
    private CategoryService categoryService;

    @Operation(summary = "分类树")
    @GetMapping("/tree")
    public Result<List<CategoryVO>> tree() {
        return Result.success(categoryService.listTree());
    }
}
