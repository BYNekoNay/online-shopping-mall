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

    @Resource
    private com.pzhu.mall.modules.admin.service.OperationLogService operationLogService;

    @Operation(summary = "商品列表")
    @GetMapping
    public Result<PageResult<ProductVO>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<Product> page = new Page<>(pageNum, pageSize);
        var qw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Product>();
        qw.eq(Product::getIsDeleted, 0);
        PageResult<Product> result = PageResult.of(productMapper.selectPage(page, qw));
        // AD-04 修复：复用 ProductService 批量构建 VO（原逐条 toVO 存在 N+1）
        java.util.List<ProductVO> voList = productService.toVOList(result.getRecords());
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
        // AD-02 修复：检查影响行数，并发双审时返回明确错误（此前 0 行也返回成功）
        int updated = productMapper.update(null,
                new LambdaUpdateWrapper<Product>()
                        .set(Product::getStatus, newStatus)
                        .eq(Product::getId, id)
                        .eq(Product::getStatus, ProductStatus.PENDING.getCode())
        );
        if (updated == 0) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "商品已不在待审核状态");
        }
        // AD-03 修复：审核操作记录日志
        operationLogService.record(
                com.pzhu.mall.security.LoginUserContext.getCurrentUserId(),
                Boolean.TRUE.equals(dto.getApproved()) ? "商品审核通过" : "商品审核驳回",
                "商品#" + id + (dto.getReason() != null ? " 原因:" + dto.getReason() : ""));
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
        // AD-02 修复：检查影响行数，重复下架/并发下架返回明确错误
        int updated = productMapper.update(null,
                new LambdaUpdateWrapper<Product>()
                        .set(Product::getStatus, ProductStatus.OFFLINE.getCode())
                        .eq(Product::getId, id)
                        .eq(Product::getStatus, ProductStatus.ONLINE.getCode())
        );
        if (updated == 0) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "商品不在已上架状态，无法下架");
        }
        // AD-03 修复：下架操作记录日志
        operationLogService.record(
                com.pzhu.mall.security.LoginUserContext.getCurrentUserId(),
                "商品下架",
                "商品#" + id);
        return Result.success();
    }

    public static class AuditDTO {
        @javax.validation.constraints.NotNull(message = "approved 不能为空")
        private Boolean approved;
        private String reason;
        public Boolean getApproved() { return approved; }
        public void setApproved(Boolean approved) { this.approved = approved; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
