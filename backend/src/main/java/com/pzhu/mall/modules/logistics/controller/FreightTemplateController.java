package com.pzhu.mall.modules.logistics.controller;

import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.modules.logistics.entity.FreightTemplate;
import com.pzhu.mall.modules.logistics.service.FreightService;
import com.pzhu.mall.modules.logistics.service.LogisticsQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;

/**
 * 运费模板控制器（商家端）。
 */
@Tag(name = "运费模板")
@RestController
@RequestMapping("/api/merchant/freight-templates")
public class FreightTemplateController {

    @Resource
    private FreightService freightService;

    @Operation(summary = "运费模板列表")
    @GetMapping
    public Result<List<FreightTemplate>> list() {
        Long shopId = getShopId();
        return Result.success(freightService.listByShop(shopId));
    }

    @Operation(summary = "保存运费模板")
    @PostMapping
    public Result<Void> save(@RequestBody FreightTemplate template) {
        Long shopId = getShopId();
        template.setShopId(shopId);
        freightService.save(template);
        return Result.success();
    }

    @Operation(summary = "计算运费")
    @GetMapping("/calculate")
    public Result<BigDecimal> calculate(@RequestParam Long shopId,
                                        @RequestParam String province,
                                        @RequestParam BigDecimal goodsAmount) {
        return Result.success(freightService.calculate(shopId, province, goodsAmount));
    }

    private Long getShopId() {
        Long userId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();
        // TODO: 注入 ShopService 获取 shopId
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
