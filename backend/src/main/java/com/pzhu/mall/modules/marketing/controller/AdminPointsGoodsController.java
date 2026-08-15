package com.pzhu.mall.modules.marketing.controller;

import com.pzhu.mall.common.result.PageResult;
import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.modules.marketing.entity.PointsGoods;
import com.pzhu.mall.modules.marketing.service.PointsExchangeService;
import com.pzhu.mall.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 管理员端积分商城商品管理（C-1）。
 */
@Tag(name = "管理员-积分商城")
@RestController
@RequestMapping("/api/admin/points-goods")
@RequireRole(3)
public class AdminPointsGoodsController {

    @Resource
    private PointsExchangeService pointsExchangeService;

    @Operation(summary = "兑换商品列表（管理端）")
    @GetMapping
    public Result<PageResult<PointsGoods>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                                @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(pointsExchangeService.adminList(pageNum, pageSize));
    }

    @Operation(summary = "创建兑换商品")
    @PostMapping
    public Result<Void> create(@RequestBody PointsGoods goods) {
        pointsExchangeService.create(goods);
        return Result.success();
    }

    @Operation(summary = "更新兑换商品")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody PointsGoods goods) {
        goods.setId(id);
        pointsExchangeService.update(goods);
        return Result.success();
    }

    @Operation(summary = "删除兑换商品（软删）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        pointsExchangeService.delete(id);
        return Result.success();
    }
}
