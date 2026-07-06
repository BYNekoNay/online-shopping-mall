package com.pzhu.mall.modules.admin.controller;

import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.common.result.PageResult;
import com.pzhu.mall.modules.product.entity.Product;
import com.pzhu.mall.modules.product.mapper.ProductMapper;
import com.pzhu.mall.security.RequireRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 管理员端商品管理控制器。
 */
@Tag(name = "管理员-商品管理")
@RestController
@RequestMapping("/api/admin/products")
@RequireRole(3)
public class AdminProductController {

    @Resource
    private ProductMapper productMapper;

    @Operation(summary = "商品列表")
    @GetMapping
    public Result<PageResult<Product>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<Product> page = new Page<>(pageNum, pageSize);
        return Result.success(PageResult.of(productMapper.selectPage(page, null)));
    }

    @Operation(summary = "审核商品")
    @PutMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @RequestBody AuditDTO dto) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            return Result.error(404, "Product not found");
        }
        product.setStatus(dto.getApproved() ? 1 : 3);
        productMapper.updateById(product);
        return Result.success();
    }

    @Operation(summary = "下架商品")
    @PutMapping("/{id}/offline")
    public Result<Void> offline(@PathVariable Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            return Result.error(404, "Product not found");
        }
        product.setStatus(0);
        productMapper.updateById(product);
        return Result.success();
    }

    public static class AuditDTO {
        private Boolean approved;
        private String reason;
        public Boolean getApproved() { return approved; }
        public void setApproved(Boolean approved) { this.approved = approved; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
