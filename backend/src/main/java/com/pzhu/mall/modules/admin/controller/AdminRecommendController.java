package com.pzhu.mall.modules.admin.controller;

import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.modules.recommend.service.RecommendCalculateService;
import com.pzhu.mall.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 推荐管理后台控制器。
 */
@Tag(name = "推荐管理（管理员）")
@RestController
@RequestMapping("/api/admin/recommend")
@RequireRole(3)
public class AdminRecommendController {

    @Resource
    private RecommendCalculateService recommendCalculateService;

    @Operation(summary = "手动触发推荐结果全量刷新")
    @PostMapping("/refresh")
    public Result<String> refresh() {
        recommendCalculateService.calculateForAll();
        return Result.success("推荐结果已刷新");
    }
}
