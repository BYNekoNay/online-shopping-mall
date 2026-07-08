package com.pzhu.mall.modules.admin.controller;

import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.common.result.PageResult;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.modules.admin.entity.Dict;
import com.pzhu.mall.modules.admin.entity.OperationLog;
import com.pzhu.mall.modules.admin.mapper.DictMapper;
import com.pzhu.mall.modules.admin.mapper.OperationLogMapper;
import com.pzhu.mall.modules.admin.service.OperationLogService;
import com.pzhu.mall.security.LoginUserContext;
import com.pzhu.mall.security.RequireRole;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员端系统管理控制器（操作日志 + 数据字典）。
 */
@Tag(name = "管理员-系统管理")
@RestController
@RequestMapping("/api/admin/system")
@RequireRole(3)
public class AdminSystemController {

    @Resource
    private OperationLogMapper operationLogMapper;

    @Resource
    private DictMapper dictMapper;

    @Resource
    private OperationLogService operationLogService;

    @Operation(summary = "操作日志列表")
    @GetMapping("/logs")
    public Result<PageResult<OperationLog>> logs(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        var qw = new LambdaQueryWrapper<OperationLog>()
                .orderByDesc(OperationLog::getCreateTime);

        if (operatorId != null) {
            qw.eq(OperationLog::getOperatorId, operatorId);
        }
        if (startTime != null && endTime != null) {
            try {
                LocalDateTime s = LocalDateTime.parse(startTime + " 00:00:00");
                LocalDateTime e = LocalDateTime.parse(endTime + " 23:59:59");
                qw.between(OperationLog::getCreateTime, s, e);
            } catch (java.time.format.DateTimeParseException ex) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "日期格式错误，正确格式：yyyy-MM-dd");
            }
        }

        Page<OperationLog> page = new Page<>(pageNum, pageSize);
        var result = operationLogMapper.selectPage(page, qw);
        return Result.success(PageResult.of(result));
    }

    @Operation(summary = "数据字典列表")
    @GetMapping("/dicts")
    public Result<List<Dict>> dicts() {
        return Result.success(dictMapper.selectList(null));
    }

    @Operation(summary = "创建字典项")
    @PostMapping("/dicts")
    public Result<Long> createDict(@RequestBody Dict dict) {
        dictMapper.insert(dict);
        Long operatorId = LoginUserContext.getCurrentUserId();
        operationLogService.record(operatorId, "创建字典项:" + dict.getDictKey(), "字典#" + dict.getId());
        return Result.success(dict.getId());
    }

    @Operation(summary = "更新字典项")
    @PutMapping("/dicts/{id}")
    public Result<Void> updateDict(@PathVariable Long id, @RequestBody Dict dict) {
        dict.setId(id);
        dictMapper.updateById(dict);
        Long operatorId = LoginUserContext.getCurrentUserId();
        operationLogService.record(operatorId, "更新字典项:" + dict.getDictKey(), "字典#" + id);
        return Result.success();
    }

    @Operation(summary = "删除字典项")
    @DeleteMapping("/dicts/{id}")
    public Result<Void> deleteDict(@PathVariable Long id) {
        dictMapper.deleteById(id);
        Long operatorId = LoginUserContext.getCurrentUserId();
        operationLogService.record(operatorId, "删除字典项", "字典#" + id);
        return Result.success();
    }
}
