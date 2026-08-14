package com.pzhu.mall.modules.admin.service;

import com.pzhu.mall.modules.admin.entity.OperationLog;
import com.pzhu.mall.modules.admin.mapper.OperationLogMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * 操作日志服务。
 */
@Service
public class OperationLogService {

    @Resource
    private OperationLogMapper operationLogMapper;

    /**
     * 记录操作日志（默认管理员角色）。
     */
    public void record(Long operatorId, String operation, String target) {
        record(operatorId, 3, operation, target);
    }

    /**
     * 记录操作日志（AD-06 修复：operatorRole 参数化，支持商家等非管理员操作方）。
     */
    public void record(Long operatorId, Integer operatorRole, String operation, String target) {
        OperationLog log = new OperationLog();
        log.setOperatorId(operatorId);
        log.setOperatorRole(operatorRole != null ? operatorRole : 3);
        log.setOperation(operation);
        log.setTarget(target);
        log.setCreateTime(LocalDateTime.now());
        operationLogMapper.insert(log);
    }
}
