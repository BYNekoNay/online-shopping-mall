package com.pzhu.mall.modules.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pzhu.mall.modules.admin.entity.SystemConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SystemConfigMapper extends BaseMapper<SystemConfig> {

    @Select("SELECT config_value FROM system_config WHERE config_key = #{key} LIMIT 1")
    String selectValueByKey(@Param("key") String key);
}
