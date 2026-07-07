package com.pzhu.mall.modules.admin.controller;

import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.modules.admin.entity.SystemConfig;
import com.pzhu.mall.modules.admin.service.SystemConfigService;
import com.pzhu.mall.modules.admin.service.OperationLogService;
import com.pzhu.mall.security.LoginUserContext;
import com.pzhu.mall.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 管理员端系统参数配置控制器。
 */
@Tag(name = "管理员-系统配置")
@RestController
@RequestMapping("/api/admin/system/config")
@RequireRole(3)
public class AdminConfigController {

    @Resource
    private SystemConfigService systemConfigService;

    @Resource
    private OperationLogService operationLogService;

    @Operation(summary = "获取指定配置")
    @GetMapping("/{key}")
    public Result<String> get(@PathVariable String key) {
        String value = systemConfigService.get(key);
        return Result.success(value);
    }

    @Operation(summary = "获取全部配置")
    @GetMapping
    public Result<?> list() {
        return Result.success(systemConfigService.getAll());
    }

    @Operation(summary = "更新配置（upsert）")
    @PutMapping("/{key}")
    public Result<Void> update(@PathVariable String key, @RequestBody ConfigDTO dto) {
        systemConfigService.upsert(key, dto.getValue(), dto.getDescription());
        Long operatorId = LoginUserContext.getCurrentUserId();
        operationLogService.record(operatorId, "更新系统配置:" + key, "系统配置#" + key);
        return Result.success();
    }

    public static class ConfigDTO {
        private String value;
        private String description;
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
