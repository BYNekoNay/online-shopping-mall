package com.pzhu.mall.modules.admin.controller;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.modules.admin.entity.Dict;
import com.pzhu.mall.modules.admin.entity.OperationLog;
import com.pzhu.mall.modules.admin.mapper.DictMapper;
import com.pzhu.mall.modules.admin.mapper.OperationLogMapper;
import com.pzhu.mall.modules.admin.service.OperationLogService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AdminSystemController 单元测试（操作日志/字典管理）。
 */
class AdminSystemControllerTest {

    private OperationLogMapper operationLogMapper;
    private DictMapper dictMapper;
    private OperationLogService operationLogService;
    private AdminSystemController controller;

    @BeforeAll
    static void initTableInfo() {
        for (Class<?> entity : new Class<?>[]{OperationLog.class, Dict.class}) {
            if (!TableInfoHelper.getTableInfos().stream()
                    .anyMatch(t -> t.getEntityType() == entity)) {
                TableInfoHelper.initTableInfo(
                        new MapperBuilderAssistant(new MybatisConfiguration(), ""), entity);
            }
        }
    }

    @BeforeEach
    void setUp() {
        operationLogMapper = mock(OperationLogMapper.class);
        dictMapper = mock(DictMapper.class);
        operationLogService = mock(OperationLogService.class);
        controller = new AdminSystemController();
        inject(controller, "operationLogMapper", operationLogMapper);
        inject(controller, "dictMapper", dictMapper);
        inject(controller, "operationLogService", operationLogService);
    }

    @Test
    void logs_returnsPagedLogs() {
        Page<OperationLog> page = new Page<>(1, 10);
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        when(operationLogMapper.selectPage(any(Page.class), any())).thenReturn(page);

        var result = controller.logs(1, 10, null, null, null);

        assertNotNull(result.getData());
        verify(operationLogMapper).selectPage(any(Page.class), any());
    }

    @Test
    void logs_invalidDateRange_throws() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.logs(1, 10, null, "2026-13-99", "2026-08-31"));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void dicts_returnsList() {
        when(dictMapper.selectList(any())).thenReturn(Collections.singletonList(new Dict()));

        List<Dict> result = controller.dicts().getData();

        assertEquals(1, result.size());
        verify(dictMapper).selectList(any());
    }

    @Test
    void createDict_delegates() {
        Dict dict = new Dict();
        when(dictMapper.insert(dict)).thenReturn(1);

        var result = controller.createDict(dict);

        assertNotNull(result);
        verify(dictMapper).insert(dict);
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
