package com.pzhu.mall.modules.admin.controller;

import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.common.result.PageResult;
import com.pzhu.mall.common.enums.ProductStatus;
import com.pzhu.mall.modules.product.entity.Product;
import com.pzhu.mall.modules.product.mapper.ProductMapper;
import com.pzhu.mall.modules.product.vo.ProductVO;
import com.pzhu.mall.modules.product.service.ProductService;
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
    private ProductService productService;

    @Resource
    private ProductMapper productMapper;

    @Operation(summary = "商品列表")
    @GetMapping
    public Result<PageResult<ProductVO>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<Product> page = new Page<>(pageNum, pageSize);
        PageResult<Product> result = PageResult.of(productMapper.selectPage(page, null));
        // Convert to VO
        java.util.List<ProductVO> voList = result.getRecords().stream()
                .map(productService::toVO)
                .collect(java.util.stream.Collectors.toList());
        return Result.success(new PageResult<>(result.getTotal(), pageNum, pageSize, result.getPages(), voList));
    }

    @Operation(summary = "审核商品")
    @PutMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @RequestBody AuditDTO dto) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            return Result.error(404, "Product not found");
        }
        product.setStatus(dto.getApproved() ? ProductStatus.ONLINE.getCode() : ProductStatus.REJECTED.getCode());
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
        product.setStatus(ProductStatus.OFFLINE.getCode());
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
