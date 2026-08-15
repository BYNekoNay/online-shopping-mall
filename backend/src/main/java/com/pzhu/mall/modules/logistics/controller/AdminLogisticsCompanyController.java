package com.pzhu.mall.modules.logistics.controller;

import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.modules.logistics.entity.LogisticsCompany;
import com.pzhu.mall.modules.logistics.service.LogisticsCompanyService;
import com.pzhu.mall.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 物流公司字典控制器（C-4）。
 */
@Tag(name = "物流公司字典")
@RestController
@RequestMapping("/api/admin/logistics-companies")
@RequireRole(3)
public class AdminLogisticsCompanyController {

    @Resource
    private LogisticsCompanyService logisticsCompanyService;

    @Operation(summary = "物流公司列表（管理端）")
    @GetMapping
    public Result<List<LogisticsCompany>> list() {
        return Result.success(logisticsCompanyService.adminList());
    }

    @Operation(summary = "新增物流公司")
    @PostMapping
    public Result<Void> create(@RequestBody LogisticsCompany company) {
        logisticsCompanyService.create(company);
        return Result.success();
    }

    @Operation(summary = "更新物流公司")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody LogisticsCompany company) {
        company.setId(id);
        logisticsCompanyService.update(company);
        return Result.success();
    }

    @Operation(summary = "删除物流公司")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        logisticsCompanyService.delete(id);
        return Result.success();
    }
}
