package com.pzhu.mall.modules.product.controller;

import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.common.result.PageResult;
import com.pzhu.mall.modules.product.dto.ProductQueryDTO;
import com.pzhu.mall.modules.product.entity.Product;
import com.pzhu.mall.modules.product.entity.Review;
import com.pzhu.mall.modules.product.service.ProductService;
import com.pzhu.mall.modules.product.service.CategoryService;
import com.pzhu.mall.modules.product.service.ReviewService;
import com.pzhu.mall.modules.product.vo.ProductVO;
import com.pzhu.mall.modules.product.vo.CategoryVO;
import com.pzhu.mall.modules.product.service.ReviewService.ProductRatingVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    @Resource
    private ReviewService reviewService;

    @Operation(summary = "商品列表/搜索（search 参数由 ProductQueryDTO.keyword 传递）")
    @GetMapping
    public Result<PageResult<ProductVO>> list(ProductQueryDTO query) {
        // M-15 修复：消费者端列表仅展示上架商品，避免待审核/下架商品出现在搜索结果（商家端走 /api/merchant/products）
        query.setStatus(com.pzhu.mall.common.enums.ProductStatus.ONLINE.getCode());
        return Result.success(productService.listPage(query));
    }

    /** @deprecated search 端点与 list 完全重复，保留仅用于前端兼容，后续应统一使用 GET /api/products */
    @Operation(summary = "商品搜索（别名，等价于 list）")
    @GetMapping("/search")
    public Result<PageResult<ProductVO>> search(ProductQueryDTO query) {
        return list(query);
    }

    @Operation(summary = "我的搜索历史（D-3，需登录，去重前10条）")
    @GetMapping("/search/history")
    public Result<List<String>> searchHistory(@RequestParam(defaultValue = "10") Integer limit) {
        Long userId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();
        return Result.success(productService.listSearchHistory(userId, limit));
    }

    @Operation(summary = "清空我的搜索历史（D-3，需登录）")
    @DeleteMapping("/search/history")
    public Result<Void> clearSearchHistory() {
        Long userId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();
        productService.clearSearchHistory(userId);
        return Result.success();
    }

    @Operation(summary = "商品详情")
    @GetMapping("/{id}")
    public Result<ProductVO> detail(@PathVariable Long id) {
        // P-01/P-03 修复：消费者视角校验 ONLINE + 记录浏览行为
        return Result.success(productService.getDetail(id, true));
    }

    @Operation(summary = "分类树")
    @GetMapping("/categories/tree")
    public Result<List<CategoryVO>> categories() {
        return Result.success(categoryService.listTree());
    }

    @Operation(summary = "商品评价列表（分页）")
    @GetMapping("/{id}/reviews")
    public Result<PageResult<Review>> reviews(@PathVariable Long id,
                                              @RequestParam(defaultValue = "1") long pageNum,
                                              @RequestParam(defaultValue = "20") long pageSize) {
        // H-22 修复：分页返回，pageSize 上限 100，防止热门商品评价全量加载
        if (pageSize > 100) {
            pageSize = 100;
        }
        if (pageNum < 1) {
            pageNum = 1;
        }
        return Result.success(reviewService.listByProduct(id, pageNum, pageSize));
    }

    @Operation(summary = "商品评分统计（平均分+评价数）")
    @GetMapping("/{id}/rating")
    public Result<ProductRatingVO> rating(@PathVariable Long id) {
        return Result.success(reviewService.getProductRating(id));
    }
}
