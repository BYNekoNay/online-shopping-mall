package com.pzhu.mall.modules.order.controller;

import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.modules.order.dto.RefundApplyDTO;
import com.pzhu.mall.modules.order.dto.RefundAuditDTO;
import com.pzhu.mall.modules.order.service.RefundService;
import com.pzhu.mall.modules.order.vo.RefundVO;
import com.pzhu.mall.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 退款/退货控制器。
 */
@Tag(name = "退款退货")
@RestController
@RequestMapping("/api/refunds")
@RequireRole(1)
public class RefundController {

    @Resource
    private RefundService refundService;

    @Operation(summary = "申请退款")
    @PostMapping
    public Result<Void> apply(@Validated @RequestBody RefundApplyDTO dto) {
        refundService.apply(dto);
        return Result.success();
    }

    @Operation(summary = "我的退款列表")
    @GetMapping
    public Result<List<RefundVO>> list() {
        return Result.success(refundService.listByUser());
    }

    @Operation(summary = "商家审核退款")
    @RequireRole(2)
    @PutMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @Validated @RequestBody RefundAuditDTO dto) {
        refundService.audit(id, dto.getApproved(), dto.getHandleRemark());
        return Result.success();
    }
}
