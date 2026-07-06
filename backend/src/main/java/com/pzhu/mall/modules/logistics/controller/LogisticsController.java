package com.pzhu.mall.modules.logistics.controller;

import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.modules.logistics.service.LogisticsQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 物流查询控制器。
 */
@Tag(name = "物流查询")
@RestController
@RequestMapping("/api/logistics")
public class LogisticsController {

    @Resource
    private LogisticsQueryService logisticsQueryService;

    @Operation(summary = "查询物流轨迹")
    @GetMapping("/{orderId}")
    public Result<String> query(@PathVariable Long orderId) {
        return Result.success(logisticsQueryService.query(orderId));
    }
}
