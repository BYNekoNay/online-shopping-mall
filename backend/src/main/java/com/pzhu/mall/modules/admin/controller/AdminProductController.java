package com.pzhu.mall.modules.admin.controller;

import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.common.result.PageResult;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.common.enums.ProductStatus;
import com.pzhu.mall.modules.product.entity.Product;
import com.pzhu.mall.modules.product.mapper.ProductMapper;
import com.pzhu.mall.modules.product.vo.ProductVO;
import com.pzhu.mall.modules.product.service.ProductService;
import com.pzhu.mall.security.RequireRole;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.transaction.annotation.Transactional;
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
        var qw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Product>();
        qw.eq(Product::getIsDeleted, 0);
        PageResult<Product> result = PageResult.of(productMapper.selectPage(page, qw));
        // Convert to VO
        java.util.List<ProductVO> voList = result.getRecords().stream()
                .map(productService::toVO)
                .collect(java.util.stream.Collectors.toList());
        return Result.success(new PageResult<>(result.getTotal(), pageNum, pageSize, result.getPages(), voList));
    }

    @Operation(summary = "审核商品")
    @PutMapping("/{id}/audit")
    @Transactional
    public Result<Void> audit(@PathVariable Long id, @RequestBody AuditDTO dto) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        // R3-C1: 仅 PENDING 状态可审核
        if (product.getStatus() != ProductStatus.PENDING.getCode()) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        int newStatus = Boolean.TRUE.equals(dto.getApproved()) ? ProductStatus.ONLINE.getCode() : ProductStatus.REJECTED.getCode();
        // R3-C2: 原子更新 WHERE status=PENDING
        productMapper.update(null,
                new LambdaUpdateWrapper<Product>()
                        .set(Product::getStatus, newStatus)
                        .eq(Product::getId, id)
                        .eq(Product::getStatus, ProductStatus.PENDING.getCode())
        );
        return Result.success();
    }

    @Operation(summary = "下架商品")
    @PutMapping("/{id}/offline")
    @Transactional
    public Result<Void> offline(@PathVariable Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        productMapper.update(null,
                new LambdaUpdateWrapper<Product>()
                        .set(Product::getStatus, ProductStatus.OFFLINE.getCode())
                        .eq(Product::getId, id)
                        .eq(Product::getStatus, ProductStatus.ONLINE.getCode())
        );
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
