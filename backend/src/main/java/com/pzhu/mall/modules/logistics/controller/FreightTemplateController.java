package com.pzhu.mall.modules.logistics.controller;

import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.modules.logistics.entity.FreightTemplate;
import com.pzhu.mall.modules.logistics.service.FreightService;
import com.pzhu.mall.modules.logistics.service.LogisticsQueryService;
import com.pzhu.mall.modules.shop.service.ShopService;
import com.pzhu.mall.security.LoginUserContext;
import com.pzhu.mall.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;

/**
 * 运费模板控制器（商家端 + 消费者计算 + 物流查询）。
 */
@Tag(name = "运费与物流")
@RestController
@RequestMapping("/api")
public class FreightTemplateController {

    @Resource
    private FreightService freightService;

    @Resource
    private ShopService shopService;

    @Resource
    private LogisticsQueryService logisticsQueryService;

    // ==================== 商家端 ====================

    @Operation(summary = "运费模板列表（商家）")
    @RequireRole(2)
    @GetMapping("/merchant/freight-templates")
    public Result<List<FreightTemplate>> merchantList() {
        Long shopId = getShopId();
        return Result.success(freightService.listByShop(shopId));
    }

    @Operation(summary = "保存运费模板（商家）")
    @RequireRole(2)
    @PostMapping("/merchant/freight-templates")
    public Result<Void> merchantSave(@RequestBody FreightTemplate template) {
        Long shopId = getShopId();
        template.setShopId(shopId);
        freightService.save(template);
        return Result.success();
    }

    @Operation(summary = "计算运费")
    @GetMapping("/merchant/freight-templates/calculate")
    public Result<BigDecimal> calculate(@RequestParam Long shopId,
                                        @RequestParam String province,
                                        @RequestParam BigDecimal goodsAmount) {
        return Result.success(freightService.calculate(shopId, province, goodsAmount));
    }

    // ==================== 物流查询 ====================

    @Operation(summary = "查询物流轨迹")
    @GetMapping("/logistics/{orderId}/track")
    public Result<String> track(@PathVariable Long orderId) {
        try {
            String result = logisticsQueryService.query(orderId);
            return Result.success(result);
        } catch (Exception e) {
            // 第三方查询失败时降级返回提示，不抛出异常中断请求
            return Result.success("{\"status\":\"物流信息暂不可用\",\"tracks\":[]}");
        }
    }

    // ==================== 工具方法 ====================

    private Long getShopId() {
        Long userId = LoginUserContext.getCurrentUserId();
        return shopService.getMerchantShopIdOrThrow(userId);
    }
}
