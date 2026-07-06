package com.pzhu.mall.modules.order.controller;

import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.modules.order.dto.CreateOrderDTO;
import com.pzhu.mall.modules.order.service.OrderService;
import com.pzhu.mall.modules.order.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 订单控制器。
 */
@Tag(name = "订单")
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Resource
    private OrderService orderService;

    @Operation(summary = "提交订单")
    @PostMapping
    public Result<List<OrderVO>> create(@RequestBody CreateOrderDTO dto) {
        return Result.success(orderService.createOrder(dto));
    }

    @Operation(summary = "订单列表")
    @GetMapping
    public Result<List<OrderVO>> list() {
        return Result.success(orderService.listByUser());
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

    @Operation(summary = "确认收货")
    @PutMapping("/{id}/confirm")
    public Result<Void> confirm(@PathVariable Long id) {
        orderService.confirmReceive(id);
        return Result.success();
    }

    @Operation(summary = "支付订单（模拟）")
    @PostMapping("/{id}/pay")
    public Result<Void> pay(@PathVariable Long id, @RequestBody PayDTO dto) {
        orderService.pay(id, dto.getPayType());
        return Result.success();
    }

    @Operation(summary = "评价订单项")
    @PostMapping("/items/{orderItemId}/review")
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
}
