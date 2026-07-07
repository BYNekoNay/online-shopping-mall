package com.pzhu.mall.modules.admin.service;

import com.pzhu.mall.modules.admin.entity.SystemConfig;
import com.pzhu.mall.modules.admin.mapper.SystemConfigMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 系统参数配置服务。
 */
@Service
public class SystemConfigService {

    private static final Logger log = LoggerFactory.getLogger(SystemConfigService.class);

    @Resource
    private SystemConfigMapper systemConfigMapper;

    @Resource
    private OperationLogService operationLogService;

    /**
     * 获取指定 key 的配置值。
     */
    public String get(String key) {
        return systemConfigMapper.selectValueByKey(key);
    }

    /**
     * 获取全部配置（以 Map 形式）。
     */
    public java.util.Map<String, String> getAll() {
        List<SystemConfig> list = systemConfigMapper.selectList(null);
        java.util.Map<String, String> map = new java.util.HashMap<>();
        for (SystemConfig c : list) {
            if (c.getConfigKey() != null) {
                map.put(c.getConfigKey(), c.getConfigValue());
            }
        }
        return map;
    }

    /**
     * 更新或创建配置（upsert）。
     */
    @Transactional
    public void upsert(String key, String value, String description) {
        SystemConfig exist = systemConfigMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SystemConfig>()
                        .eq(SystemConfig::getConfigKey, key)
                        .last("LIMIT 1")
        );
        if (exist != null) {
            exist.setConfigValue(value);
            exist.setDescription(description);
            exist.setUpdateTime(LocalDateTime.now());
            systemConfigMapper.updateById(exist);
        } else {
            exist = new SystemConfig();
            exist.setConfigKey(key);
            exist.setConfigValue(value);
            exist.setDescription(description);
            exist.setUpdateTime(LocalDateTime.now());
            systemConfigMapper.insert(exist);
        }
        log.info("[系统配置] 更新 key={}, value={}", key, value);
    }
}
