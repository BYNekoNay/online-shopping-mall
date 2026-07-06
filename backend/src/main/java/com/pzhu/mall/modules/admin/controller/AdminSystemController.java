package com.pzhu.mall.modules.admin.controller;

import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.common.result.PageResult;
import com.pzhu.mall.modules.admin.entity.Dict;
import com.pzhu.mall.modules.admin.entity.OperationLog;
import com.pzhu.mall.modules.admin.mapper.DictMapper;
import com.pzhu.mall.modules.admin.mapper.OperationLogMapper;
import com.pzhu.mall.modules.admin.service.OperationLogService;
import com.pzhu.mall.security.RequireRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 管理员端系统管理控制器。
 */
@Tag(name = "管理员-系统管理")
@RestController
@RequestMapping("/api/admin/system")
@RequireRole(3)
public class AdminSystemController {

    @Resource
    private DictMapper dictMapper;

    @Resource
    private OperationLogMapper operationLogMapper;

    @Resource
    private OperationLogService operationLogService;

    @Operation(summary = "操作日志列表")
    @GetMapping("/logs")
    public Result<PageResult<OperationLog>> logs(@RequestParam(defaultValue = "1") Integer pageNum,
                                                 @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<OperationLog> page = new Page<>(pageNum, pageSize);
        return Result.success(PageResult.of(operationLogMapper.selectPage(page, null)));
    }

    @Operation(summary = "数据字典列表")
    @GetMapping("/dicts")
    public Result<List<Dict>> dicts() {
        return Result.success(dictMapper.selectList(null));
    }

    @Operation(summary = "创建字典项")
    @PostMapping("/dicts")
    public Result<Void> createDict(@RequestBody Dict dict) {
        dictMapper.insert(dict);
        return Result.success();
    }

    @Operation(summary = "记录操作日志")
    @PostMapping("/logs")
    public Result<Void> recordLog(@RequestBody LogDTO dto) {
        operationLogService.record(dto.getOperatorId(), dto.getOperation(), dto.getTarget());
        return Result.success();
    }

    public static class LogDTO {
        private Long operatorId;
        private String operation;
        private String target;
        public Long getOperatorId() { return operatorId; }
        public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
        public String getOperation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }
        public String getTarget() { return target; }
        public void setTarget(String target) { this.target = target; }
    }
}
