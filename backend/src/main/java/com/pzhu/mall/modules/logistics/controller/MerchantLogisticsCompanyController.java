package com.pzhu.mall.modules.logistics.controller;

import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.modules.logistics.entity.LogisticsCompany;
import com.pzhu.mall.modules.logistics.service.LogisticsCompanyService;
import com.pzhu.mall.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 物流公司字典-商家端（发货下拉，C-4）。
 */
@Tag(name = "物流公司-商家端")
@RestController
@RequestMapping("/api/merchant/logistics-companies")
@RequireRole(2)
public class MerchantLogisticsCompanyController {

    @Resource
    private LogisticsCompanyService logisticsCompanyService;

    @Operation(summary = "物流公司下拉列表（仅启用）")
    @GetMapping
    public Result<List<LogisticsCompany>> list() {
        return Result.success(logisticsCompanyService.listEnabled());
    }
}
