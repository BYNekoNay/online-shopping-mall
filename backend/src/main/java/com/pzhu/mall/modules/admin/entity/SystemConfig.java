package com.pzhu.mall.modules.admin.entity;

import java.time.LocalDateTime;

/**
 * 系统参数配置实体（对应 system_config 表）。
 */
public class SystemConfig {

    private String configKey;
    private String configValue;
    private String description;
    private LocalDateTime updateTime;

    public String getConfigKey() { return configKey; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }
    public String getConfigValue() { return configValue; }
    public void setConfigValue(String configValue) { this.configValue = configValue; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
