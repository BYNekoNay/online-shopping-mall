package com.pzhu.mall.modules.logistics.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.modules.logistics.entity.LogisticsCompany;
import com.pzhu.mall.modules.logistics.mapper.LogisticsCompanyMapper;
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
 * LogisticsCompanyService 单元测试（C-4 物流公司字典）。
 * <p>覆盖 L-01~L-03：CRUD / 启用过滤 / 校验。</p>
 */
class LogisticsCompanyServiceTest {

    private LogisticsCompanyMapper mapper;
    private LogisticsCompanyService service;

    @BeforeAll
    static void initTableInfo() {
        var assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, LogisticsCompany.class);
    }

    @BeforeEach
    void setUp() {
        mapper = mock(LogisticsCompanyMapper.class);
        service = new LogisticsCompanyService();
        inject(service, "logisticsCompanyMapper", mapper);
    }

    @Test
    void listEnabled_onlyStatusOne() {
        // L-03：仅启用公司（商家下拉）
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(company(1L, "顺丰", "SF", 1)));

        List<LogisticsCompany> list = service.listEnabled();

        assertEquals(1, list.size());
        verify(mapper).selectList(any());
    }

    @Test
    void create_validCompany_inserts() {
        // L-01：合法创建
        service.create(company(null, "圆通", "YTO", 1));
        verify(mapper).insert(any(LogisticsCompany.class));
    }

    @Test
    void create_blankName_throws() {
        // L-01 校验：名称空 → 抛 10001
        LogisticsCompany c = company(null, "", "YTO", 1);
        assertThrows(BusinessException.class, () -> service.create(c));
        verify(mapper, never()).insert(any());
    }

    @Test
    void create_blankCode_throws() {
        // L-01 校验：编码空 → 抛 10001
        LogisticsCompany c = company(null, "圆通", "", 1);
        assertThrows(BusinessException.class, () -> service.create(c));
    }

    @Test
    void update_validCompany_updates() {
        // L-01：更新
        service.update(company(1L, "中通", "ZTO", 1));
        verify(mapper).updateById(any(LogisticsCompany.class));
    }

    @Test
    void delete_removesById() {
        // L-01：删除
        service.delete(1L);
        verify(mapper).deleteById(1L);
    }

    private static LogisticsCompany company(Long id, String name, String code, int status) {
        LogisticsCompany c = new LogisticsCompany();
        c.setId(id);
        c.setName(name);
        c.setCode(code);
        c.setStatus(status);
        return c;
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
