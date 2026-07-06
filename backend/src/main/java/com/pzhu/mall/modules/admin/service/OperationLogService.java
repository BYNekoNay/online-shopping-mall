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
     * 记录操作日志。
     */
    public void record(Long operatorId, String operation, String target) {
        OperationLog log = new OperationLog();
        log.setOperatorId(operatorId);
        log.setOperatorRole(3); // 管理员
        log.setOperation(operation);
        log.setTarget(target);
        log.setCreateTime(LocalDateTime.now());
        operationLogMapper.insert(log);
    }
}
