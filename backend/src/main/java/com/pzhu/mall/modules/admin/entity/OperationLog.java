package com.pzhu.mall.modules.admin.entity;

import java.time.LocalDateTime;

/**
 * 操作日志实体（对应 operation_log 表）。
 */
public class OperationLog {

    private Long id;
    private Long operatorId;
    private Integer operatorRole;
    private String operation;
    private String target;
    private String ip;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public Integer getOperatorRole() { return operatorRole; }
    public void setOperatorRole(Integer operatorRole) { this.operatorRole = operatorRole; }
    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
