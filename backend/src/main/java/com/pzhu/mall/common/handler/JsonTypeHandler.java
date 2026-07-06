package com.pzhu.mall.common.handler;

import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import org.apache.ibatis.type.MappedTypes;

/**
 * JSON 类型处理器，用于 MyBatis-Plus 自动映射 JSON 字段。
 *
 * <p>需在实体字段上标注：
 * <pre>
 *   @TableField(typeHandler = JsonTypeHandler.class)
 *   private String regionRuleJson;
 * </pre>
 *
 * <p>依赖: mybatis-plus-spring-boot-starter 内置 JacksonTypeHandler。
 * 继承它只是为了统一包路径与显式注册 MappedTypes，避免 MyBatis 扫描遗漏。
 */
@MappedTypes(String.class)
public class JsonTypeHandler extends JacksonTypeHandler {

    public JsonTypeHandler() {
        super(String.class);
    }
}
