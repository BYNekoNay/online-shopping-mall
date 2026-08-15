package com.pzhu.mall.modules.user.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.modules.user.entity.Address;
import com.pzhu.mall.modules.user.mapper.AddressMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * AddressService 单元测试（CRUD + 默认地址规则 U-05/U-06 + IDOR 防护）。
 * <p>覆盖 docs/32 批次1 的 U-T01~06 用例。</p>
 */
class AddressServiceTest {

    private AddressMapper addressMapper;
    private AddressService service;

    @BeforeAll
    static void initTableInfo() {
        // MyBatis-Plus lambda 缓存初始化（Address::getXxx 解析需要 TableInfo）
        if (!TableInfoHelper.getTableInfos().stream()
                .anyMatch(t -> t.getEntityType() == Address.class)) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Address.class);
        }
    }

    @BeforeEach
    void setUp() {
        addressMapper = mock(AddressMapper.class);
        service = new AddressService(addressMapper);
    }

    // ==================== listByUser ====================

    @Test
    void listByUser_returnsUserAddresses() {
        when(addressMapper.selectList(any())).thenReturn(Collections.emptyList());

        assertTrue(service.listByUser(100L).isEmpty());
        verify(addressMapper).selectList(any());
    }

    // ==================== add ====================

    @Test
    void add_firstAddress_autoSetsDefault() {
        // U-06 修复验证：用户无默认地址 → 新地址自动设为默认
        Address a = new Address();
        a.setId(1L);
        a.setIsDefault(0);
        when(addressMapper.selectCount(any())).thenReturn(0L);
        when(addressMapper.insert(any())).thenReturn(1);

        Long id = service.add(100L, a);

        assertEquals(1L, id);
        assertEquals(Integer.valueOf(1), a.getIsDefault());
        assertEquals(100L, a.getUserId());
        verify(addressMapper, never()).update(any(), any());
    }

    @Test
    void add_notRequestDefault_hasExistingDefault_keepsNonDefault() {
        Address a = new Address();
        a.setId(2L);
        a.setIsDefault(0);
        when(addressMapper.selectCount(any())).thenReturn(1L);
        when(addressMapper.insert(any())).thenReturn(1);

        service.add(100L, a);

        assertEquals(Integer.valueOf(0), a.getIsDefault());
        verify(addressMapper, never()).update(any(), any());
    }

    @Test
    void add_requestDefault_clearsOthers() {
        // 显式设默认 → 先清空该用户其他默认地址
        Address a = new Address();
        a.setId(3L);
        a.setIsDefault(1);
        when(addressMapper.insert(any())).thenReturn(1);

        service.add(100L, a);

        verify(addressMapper).update(isNull(), any());
        assertEquals(Integer.valueOf(1), a.getIsDefault());
    }

    // ==================== update ====================

    @Test
    void update_notOwner_throwsNotFound() {
        // IDOR 防护：他人地址不可改
        Address exist = new Address();
        exist.setId(1L);
        exist.setUserId(999L);
        when(addressMapper.selectById(1L)).thenReturn(exist);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.update(100L, 1L, new Address()));
        assertEquals(ErrorCode.NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void update_notExist_throws() {
        when(addressMapper.selectById(1L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.update(100L, 1L, new Address()));
    }

    @Test
    void update_success_ownerCanUpdate() {
        Address exist = new Address();
        exist.setId(1L);
        exist.setUserId(100L);
        when(addressMapper.selectById(1L)).thenReturn(exist);
        Address data = new Address();
        data.setDetail("新地址");

        service.update(100L, 1L, data);

        assertEquals(1L, data.getId());
        assertEquals(100L, data.getUserId());
        verify(addressMapper).updateById(data);
    }

    @Test
    void update_setDefault_clearsOthers() {
        Address exist = new Address();
        exist.setId(1L);
        exist.setUserId(100L);
        when(addressMapper.selectById(1L)).thenReturn(exist);
        Address data = new Address();
        data.setIsDefault(1);

        service.update(100L, 1L, data);

        verify(addressMapper).update(isNull(), any());
        verify(addressMapper).updateById(data);
    }

    // ==================== delete ====================

    @Test
    void delete_notOwner_throws() {
        Address exist = new Address();
        exist.setId(1L);
        exist.setUserId(999L);
        when(addressMapper.selectById(1L)).thenReturn(exist);

        assertThrows(BusinessException.class, () -> service.delete(100L, 1L));
    }

    @Test
    void delete_defaultAddress_promotesNewest() {
        // U-05 修复验证：删除默认地址后，最近创建的补为默认
        Address exist = new Address();
        exist.setId(1L);
        exist.setUserId(100L);
        exist.setIsDefault(1);
        when(addressMapper.selectById(1L)).thenReturn(exist);

        Address next = new Address();
        next.setId(2L);
        next.setUserId(100L);
        next.setIsDefault(0);
        when(addressMapper.selectList(any())).thenReturn(Arrays.asList(next));

        service.delete(100L, 1L);

        verify(addressMapper).deleteById(1L);
        assertEquals(Integer.valueOf(1), next.getIsDefault());
        verify(addressMapper).updateById(next);
    }

    @Test
    void delete_defaultAddress_noRemaining_skipPromotion() {
        Address exist = new Address();
        exist.setId(1L);
        exist.setUserId(100L);
        exist.setIsDefault(1);
        when(addressMapper.selectById(1L)).thenReturn(exist);
        when(addressMapper.selectList(any())).thenReturn(Collections.emptyList());

        service.delete(100L, 1L);

        verify(addressMapper).deleteById(1L);
        verify(addressMapper, never()).updateById(any());
    }

    @Test
    void delete_nonDefault_simpleDelete() {
        Address exist = new Address();
        exist.setId(1L);
        exist.setUserId(100L);
        exist.setIsDefault(0);
        when(addressMapper.selectById(1L)).thenReturn(exist);

        service.delete(100L, 1L);

        verify(addressMapper).deleteById(1L);
        verify(addressMapper, never()).selectList(any());
    }
}
