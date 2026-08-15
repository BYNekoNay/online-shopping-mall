package com.pzhu.mall.modules.admin.service;

import com.pzhu.mall.modules.admin.entity.OperationLog;
import com.pzhu.mall.modules.admin.mapper.OperationLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * OperationLogService 单元测试（AD-06 角色参数化）。
 */
class OperationLogServiceTest {

    private OperationLogMapper operationLogMapper;
    private OperationLogService service;

    @BeforeEach
    void setUp() {
        operationLogMapper = mock(OperationLogMapper.class);
        service = new OperationLogService();
        inject(service, "operationLogMapper", operationLogMapper);
    }

    @Test
    void record_defaultRole_isAdmin() {
        // 默认重载：operatorRole=3（管理员）
        service.record(100L, "操作", "目标");

        var captor = org.mockito.ArgumentCaptor.forClass(OperationLog.class);
        verify(operationLogMapper).insert(captor.capture());
        assertEquals(100L, captor.getValue().getOperatorId());
        assertEquals(Integer.valueOf(3), captor.getValue().getOperatorRole());
        assertEquals("操作", captor.getValue().getOperation());
        assertEquals("目标", captor.getValue().getTarget());
        assertNotNull(captor.getValue().getCreateTime());
    }

    @Test
    void record_withMerchantRole_usesGivenRole() {
        // AD-06 修复验证：商家操作日志角色=2
        service.record(200L, 2, "上架商品", "商品#5");

        var captor = org.mockito.ArgumentCaptor.forClass(OperationLog.class);
        verify(operationLogMapper).insert(captor.capture());
        assertEquals(Integer.valueOf(2), captor.getValue().getOperatorRole());
    }

    @Test
    void record_nullRole_fallsBackToAdmin() {
        service.record(300L, null, "操作", "目标");

        var captor = org.mockito.ArgumentCaptor.forClass(OperationLog.class);
        verify(operationLogMapper).insert(captor.capture());
        assertEquals(Integer.valueOf(3), captor.getValue().getOperatorRole());
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
