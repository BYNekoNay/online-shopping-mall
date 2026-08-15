package com.pzhu.mall.modules.admin.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.pzhu.mall.modules.admin.entity.OperationLog;
import com.pzhu.mall.modules.admin.mapper.OperationLogMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * OperationLogService 单元测试（E-1 覆盖率补测：admin.service 29% → ≥80%）。
 */
class OperationLogServiceTest {

    private OperationLogMapper operationLogMapper;
    private OperationLogService service;

    @BeforeAll
    static void initTableInfo() {
        var assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, OperationLog.class);
    }

    @BeforeEach
    void setUp() {
        operationLogMapper = mock(OperationLogMapper.class);
        service = new OperationLogService();
        inject(service, "operationLogMapper", operationLogMapper);
    }

    @Test
    void record_defaultRole_isAdmin() {
        // OL-01：默认角色=3（管理员），字段完整
        service.record(100L, "禁用用户", "用户#5");
        org.mockito.ArgumentCaptor<OperationLog> captor =
                org.mockito.ArgumentCaptor.forClass(OperationLog.class);
        verify(operationLogMapper).insert(captor.capture());
        OperationLog log = captor.getValue();
        assertEquals(100L, log.getOperatorId());
        assertEquals(3, log.getOperatorRole());
        assertEquals("禁用用户", log.getOperation());
        assertEquals("用户#5", log.getTarget());
        assertNotNull(log.getCreateTime());
    }

    @Test
    void record_merchantRole_usesProvidedRole() {
        // OL-02：商家角色（2）参数化
        service.record(200L, 2, "发货", "订单#1");
        verify(operationLogMapper).insert(argThat(l ->
                l.getOperatorRole() == 2 && l.getOperatorId() == 200L && "发货".equals(l.getOperation())));
    }

    @Test
    void record_nullRole_defaultsToAdmin() {
        // OL-03：operatorRole=null → 回退 3
        service.record(100L, null, "操作", "目标");
        verify(operationLogMapper).insert(argThat(l -> l.getOperatorRole() == 3));
    }

    private static void inject(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
