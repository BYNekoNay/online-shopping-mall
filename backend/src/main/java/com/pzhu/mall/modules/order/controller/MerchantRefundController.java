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

        // 先获取该店铺所有订单 ID
        var orderQw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.pzhu.mall.modules.order.entity.Order>();
        orderQw.eq(com.pzhu.mall.modules.order.entity.Order::getShopId, shopId)
              .eq(com.pzhu.mall.modules.order.entity.Order::getIsDeleted, 0);
        List<com.pzhu.mall.modules.order.entity.Order> shopOrders = orderMapper.selectList(orderQw);
        java.util.Set<Long> shopOrderIds = shopOrders.stream()
            .map(com.pzhu.mall.modules.order.entity.Order::getId)
            .collect(java.util.stream.Collectors.toSet());

        // 分页查询所有退款，再按店铺过滤
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Refund> page =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize);
        List<Refund> pageResult = refundMapper.selectPage(
            page,
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Refund>()
                .orderByDesc(Refund::getCreateTime)
        ).getRecords();

        List<RefundVO> filtered = pageResult.stream()
            .filter(r -> shopOrderIds.contains(r.getOrderId()))
            .map(refundService::toVO)
            .collect(Collectors.toList());

        return Result.success(new PageResult<>(page.getTotal(), pageNum, pageSize, page.getPages(), filtered));
    }

    @Operation(summary = "审核退款")
    @PutMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @RequestBody AuditDTO dto) {
        refundService.audit(id, dto.getApproved(), dto.getHandleRemark());
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
