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

        // 使用子查询在数据库中过滤该店铺的退款记录，避免内存分页问题
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Refund> page =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize);
        var refundQw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Refund>();
        refundQw.inSql(Refund::getOrderId,
                "SELECT id FROM `order` WHERE shop_id = " + shopId + " AND is_deleted = 0")
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
