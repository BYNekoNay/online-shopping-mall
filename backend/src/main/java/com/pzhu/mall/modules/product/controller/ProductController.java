package com.pzhu.mall.modules.product.controller;

import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.common.result.PageResult;
import com.pzhu.mall.modules.product.dto.ProductQueryDTO;
import com.pzhu.mall.modules.product.entity.Category;
import com.pzhu.mall.modules.product.entity.Product;
import com.pzhu.mall.modules.product.service.ProductService;
import com.pzhu.mall.modules.product.service.CategoryService;
import com.pzhu.mall.modules.product.vo.ProductVO;
import com.pzhu.mall.modules.product.vo.CategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 商品控制器（消费者+商家共用）。
 */
@Tag(name = "商品")
@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Resource
    private ProductService productService;

    @Resource
    private CategoryService categoryService;

    @Operation(summary = "商品列表/搜索")
    @GetMapping
    public Result<PageResult<ProductVO>> list(ProductQueryDTO query) {
        PageResult<ProductVO> result = productService.listPage(query);
        return Result.success(result);
    }

    @Operation(summary = "商品详情")
    @GetMapping("/{id}")
    public Result<ProductVO> detail(@PathVariable Long id) {
        return Result.success(productService.getDetail(id));
    }

    @Operation(summary = "分类树")
    @GetMapping("/categories/tree")
    public Result<List<CategoryVO>> categories() {
        return Result.success(categoryService.listTree());
    }
}
