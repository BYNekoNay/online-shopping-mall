package com.pzhu.mall.modules.order.controller;

import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.modules.order.service.OrderService;
import com.pzhu.mall.modules.order.vo.OrderVO;
import com.pzhu.mall.modules.shop.service.ShopService;
import com.pzhu.mall.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 商家端订单控制器。
 */
@Tag(name = "商家-订单管理")
@RestController
@RequestMapping("/api/merchant/orders")
@RequireRole(2)
public class MerchantOrderController {

    @Resource
    private OrderService orderService;

    @Resource
    private ShopService shopService;

    @Operation(summary = "店铺订单列表")
    @GetMapping
    public Result<List<OrderVO>> list(@RequestParam(required = false) Integer status) {
        Long userId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();
        Long shopId = shopService.getMerchantShopIdOrThrow(userId);
        return Result.success(orderService.listByMerchant(shopId, status));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        Long userId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();
        Long shopId = shopService.getMerchantShopIdOrThrow(userId);
        OrderVO order = orderService.getMerchantOrderDetail(id, shopId);
        return Result.success(order);
    }

    @Operation(summary = "发货")
    @PutMapping("/{id}/ship")
    public Result<Void> ship(@PathVariable Long id, @RequestBody ShipDTO dto) {
        Long userId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();
        Long shopId = shopService.getMerchantShopIdOrThrow(userId);
        orderService.ship(id, dto.getLogisticsCompany(), dto.getTrackingNo(), shopId);
        return Result.success();
    }

    public static class ShipDTO {
        private String logisticsCompany;
        private String trackingNo;
        public String getLogisticsCompany() { return logisticsCompany; }
        public void setLogisticsCompany(String logisticsCompany) { this.logisticsCompany = logisticsCompany; }
        public String getTrackingNo() { return trackingNo; }
        public void setTrackingNo(String trackingNo) { this.trackingNo = trackingNo; }
    }
}
