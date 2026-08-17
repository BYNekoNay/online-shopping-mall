package com.pzhu.mall.modules.order.controller;

import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.modules.order.dto.CreateOrderDTO;
import com.pzhu.mall.modules.order.dto.RefundApplyDTO;
import com.pzhu.mall.modules.order.service.OrderService;
import com.pzhu.mall.modules.order.vo.OrderVO;
import com.pzhu.mall.modules.order.vo.PayVO;
import com.pzhu.mall.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 订单控制器。
 */
@Tag(name = "订单")
@RestController
@RequestMapping("/api/orders")
@RequireRole(1)
public class OrderController {

    @Resource
    private OrderService orderService;

    @Resource
    private com.pzhu.mall.modules.order.service.RefundService refundService;

    @Operation(summary = "提交订单")
    @PostMapping
    public Result<List<OrderVO>> create(@RequestBody CreateOrderDTO dto) {
        return Result.success(orderService.createOrder(dto));
    }

    @Operation(summary = "订单列表")
    @GetMapping
    public Result<List<OrderVO>> list(@RequestParam(required = false) Integer pageNum,
                                      @RequestParam(required = false) Integer pageSize,
                                      @RequestParam(required = false) Integer status) {
        // O-07 修复：支持分页参数（可选，缺省返回全量兼容既有前端）
        // FRONT-02 修复：新增 status 参数按订单状态过滤（前端订单列表 tab 筛选此前恒失效）
        return Result.success(orderService.listByUser(pageNum, pageSize, status));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        return Result.success(orderService.getDetail(id));
    }

    @Operation(summary = "取消订单")
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        orderService.cancelOrder(id);
        return Result.success();
    }

    @Operation(summary = "删除订单（B-1，仅已取消/已退款）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();
        orderService.deleteOrder(userId, id);
        return Result.success();
    }

    @Operation(summary = "确认收货")
    @PutMapping("/{id}/confirm")
    public Result<Void> confirm(@PathVariable Long id) {
        orderService.confirmReceive(id);
        return Result.success();
    }

    @Operation(summary = "支付订单（模拟）")
    @PostMapping("/{id}/pay")
    public Result<PayVO> pay(@PathVariable Long id, @RequestBody PayDTO dto) {
        orderService.pay(id, dto.getPayType());
        PayVO vo = new PayVO();
        vo.setPaySuccess(true);
        vo.setPayNo("PAY" + System.currentTimeMillis());
        return Result.success(vo);
    }

    @Operation(summary = "评价订单项")
    @PostMapping("/{orderItemId}/review")
    public Result<Void> review(@PathVariable Long orderItemId, @RequestBody ReviewDTO dto) {
        orderService.review(orderItemId, dto.getRating(), dto.getContent());
        return Result.success();
    }

    public static class PayDTO {
        private Integer payType;
        public Integer getPayType() { return payType; }
        public void setPayType(Integer payType) { this.payType = payType; }
    }

    public static class ReviewDTO {
        private Integer rating;
        private String content;
        public Integer getRating() { return rating; }
        public void setRating(Integer rating) { this.rating = rating; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    @Operation(summary = "申请退款")
    @PostMapping("/{orderId}/refund")
    public Result<Void> refund(@PathVariable Long orderId, @Validated @RequestBody RefundApplyDTO dto) {
        dto.setOrderId(orderId);
        refundService.apply(dto);
        return Result.success();
    }
}
