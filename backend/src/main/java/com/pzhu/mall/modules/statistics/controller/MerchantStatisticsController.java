package com.pzhu.mall.modules.statistics.controller;

import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.security.LoginUserContext;
import com.pzhu.mall.security.RequireRole;
import com.pzhu.mall.modules.shop.service.ShopService;
import com.pzhu.mall.modules.statistics.service.MerchantStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * 商家端数据统计控制器。
 */
@Tag(name = "商家数据统计")
@RestController
@RequestMapping("/api/merchant/statistics")
public class MerchantStatisticsController {

    @Resource
    private MerchantStatisticsService merchantStatisticsService;

    @Resource
    private ShopService shopService;

    @Operation(summary = "销售统计")
    @RequireRole({2})
    @GetMapping("/sales")
    public Result<Map<String, Object>> sales(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "day") String granularity) {
        Long userId = LoginUserContext.getCurrentUserId();
        Long shopId = shopService.getMerchantShopIdOrThrow(userId);
        Map<String, Object> data = merchantStatisticsService.getSalesStatistics(
                shopId, LocalDate.parse(startDate), LocalDate.parse(endDate), granularity);
        return Result.success(data);
    }

    @Operation(summary = "热销商品TOP10")
    @RequireRole({2})
    @GetMapping("/top-products")
    public Result<?> topProducts() {
        Long userId = LoginUserContext.getCurrentUserId();
        Long shopId = shopService.getMerchantShopIdOrThrow(userId);
        return Result.success(merchantStatisticsService.getTopProducts(shopId));
    }
}
