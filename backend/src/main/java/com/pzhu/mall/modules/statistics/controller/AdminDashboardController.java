package com.pzhu.mall.modules.statistics.controller;

import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.security.RequireRole;
import com.pzhu.mall.modules.statistics.service.PlatformStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.Map;

/**
 * 管理员端数据统计控制器。
 */
@Tag(name = "管理员数据统计")
@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    @Resource
    private PlatformStatisticsService platformStatisticsService;

    @Operation(summary = "GMV/用户增长/转化率/推荐效果总览")
    @RequireRole({3})
    @GetMapping
    public Result<Map<String, Object>> dashboard() {
        return Result.success(platformStatisticsService.getDashboard());
    }

    @Operation(summary = "PV/UV/跳出率/停留时长/转化漏斗等细项统计")
    @RequireRole({3})
    @GetMapping("/statistics/detail")
    public Result<Map<String, Object>> statisticsDetail(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        return Result.success(platformStatisticsService.getStatisticsDetail(start, end));
    }
}
