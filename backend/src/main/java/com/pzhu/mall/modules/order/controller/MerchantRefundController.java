package com.pzhu.mall.modules.order.controller;

import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.common.result.PageResult;
import com.pzhu.mall.modules.order.entity.Refund;
import com.pzhu.mall.modules.order.mapper.RefundMapper;
import com.pzhu.mall.modules.order.mapper.OrderMapper;
import com.pzhu.mall.modules.order.service.RefundService;
import com.pzhu.mall.modules.order.vo.RefundVO;
import com.pzhu.mall.modules.shop.service.ShopService;
import com.pzhu.mall.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 商家端退款/售后控制器。
 */
@Tag(name = "商家-退款管理")
@RestController
@RequestMapping("/api/merchant/refunds")
@RequireRole(2)
public class MerchantRefundController {

    @Resource
    private RefundService refundService;

    @Resource
    private ShopService shopService;

    @Resource
    private RefundMapper refundMapper;

    @Resource
    private OrderMapper orderMapper;

    @Operation(summary = "店铺退款列表（分页）")
    @GetMapping
    public Result<PageResult<RefundVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();
        Long shopId = shopService.getMerchantShopIdOrThrow(userId);

        // 先查出该店铺的订单 ID 列表（仅取 id 列），再据此过滤退款记录
        List<Long> orderIds = orderMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.pzhu.mall.modules.order.entity.Order>()
                        .select(com.pzhu.mall.modules.order.entity.Order::getId)
                        .eq(com.pzhu.mall.modules.order.entity.Order::getShopId, shopId)
        ).stream().map(com.pzhu.mall.modules.order.entity.Order::getId).collect(Collectors.toList());

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Refund> page =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize);
        if (orderIds.isEmpty()) {
            // 店铺无订单，直接返回空分页，避免空 IN 导致全表扫描
            return Result.success(new PageResult<>(0L, pageNum, pageSize, 0L, List.of()));
        }

        var refundQw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Refund>();
        refundQw.in(Refund::getOrderId, orderIds)
              .orderByDesc(Refund::getCreateTime);
        List<Refund> pageResult = refundMapper.selectPage(page, refundQw).getRecords();

        List<RefundVO> filtered = pageResult.stream()
            .map(refundService::toVO)
            .collect(Collectors.toList());

        return Result.success(new PageResult<>(page.getTotal(), pageNum, pageSize, page.getPages(), filtered));
    }

    @Operation(summary = "审核退款")
    @PutMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @RequestBody AuditDTO dto) {
        Long userId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();
        Long shopId = shopService.getMerchantShopIdOrThrow(userId);
        refundService.audit(id, dto.getApproved(), dto.getHandleRemark(), shopId);
        return Result.success();
    }

    public static class AuditDTO {
        private Boolean approved;
        private String handleRemark;

        public Boolean getApproved() { return approved; }
        public void setApproved(Boolean approved) { this.approved = approved; }
        public String getHandleRemark() { return handleRemark; }
        public void setHandleRemark(String handleRemark) { this.handleRemark = handleRemark; }
    }
}
