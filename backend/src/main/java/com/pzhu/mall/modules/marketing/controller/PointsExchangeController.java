package com.pzhu.mall.modules.marketing.controller;

import com.pzhu.mall.common.result.PageResult;
import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.modules.marketing.entity.PointsExchangeLog;
import com.pzhu.mall.modules.marketing.entity.PointsGoods;
import com.pzhu.mall.modules.marketing.service.PointsExchangeService;
import com.pzhu.mall.security.LoginUserContext;
import com.pzhu.mall.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 积分商城控制器（C-1 积分兑换商品）。
 */
@Tag(name = "积分商城")
@RestController
@RequestMapping("/api/points")
public class PointsExchangeController {

    @Resource
    private PointsExchangeService pointsExchangeService;

    @Operation(summary = "兑换商品列表（消费者）")
    @GetMapping("/goods")
    public Result<PageResult<PointsGoods>> goods(@RequestParam(defaultValue = "1") Integer pageNum,
                                                 @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(pointsExchangeService.listGoods(pageNum, pageSize));
    }

    @Operation(summary = "兑换商品（需登录）")
    @PostMapping("/exchange")
    public Result<Void> exchange(@RequestBody ExchangeDTO dto) {
        Long userId = LoginUserContext.getCurrentUserId();
        pointsExchangeService.exchange(userId, dto.getGoodsId(), dto.getQuantity() == null ? 1 : dto.getQuantity());
        return Result.success();
    }

    @Operation(summary = "我的兑换记录（需登录）")
    @GetMapping("/exchange-logs")
    public Result<List<PointsExchangeLog>> myLogs(@RequestParam(defaultValue = "10") Integer limit) {
        Long userId = LoginUserContext.getCurrentUserId();
        return Result.success(pointsExchangeService.listMyLogs(userId, limit));
    }

    public static class ExchangeDTO {
        private Long goodsId;
        private Integer quantity;
        public Long getGoodsId() { return goodsId; }
        public void setGoodsId(Long goodsId) { this.goodsId = goodsId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
}
