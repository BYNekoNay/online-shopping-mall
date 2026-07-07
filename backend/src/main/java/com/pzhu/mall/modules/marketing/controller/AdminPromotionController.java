package com.pzhu.mall.modules.marketing.controller;

import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.modules.marketing.entity.Promotion;
import com.pzhu.mall.modules.marketing.service.PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 促销活动管理端控制器。
 */
@Tag(name = "促销活动（管理端）")
@RestController
@RequestMapping("/api/admin/promotions")
public class AdminPromotionController {

    @Resource
    private PromotionService promotionService;

    @Operation(summary = "促销活动列表（管理端）")
    @GetMapping
    public Result<List<Promotion>> list() {
        List<Promotion> promotions = promotionService.listAll();
        return Result.success(promotions);
    }

    @Operation(summary = "创建促销活动")
    @PostMapping
    public Result<Void> create(@RequestBody Promotion promotion) {
        promotionService.create(promotion);
        return Result.success();
    }

    @Operation(summary = "更新促销活动")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Promotion promotion) {
        promotion.setId(id);
        promotionService.update(promotion);
        return Result.success();
    }

    @Operation(summary = "下线促销活动")
    @PutMapping("/{id}/offline")
    public Result<Void> offline(@PathVariable Long id) {
        promotionService.offline(id);
        return Result.success();
    }

    @Operation(summary = "删除促销活动")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        promotionService.delete(id);
        return Result.success();
    }
}
