package com.pzhu.mall.modules.admin.controller;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.modules.admin.service.OperationLogService;
import com.pzhu.mall.modules.shop.entity.Shop;
import com.pzhu.mall.modules.shop.mapper.ShopMapper;
import com.pzhu.mall.modules.shop.service.ShopService;
import com.pzhu.mall.modules.shop.vo.ShopVO;
import com.pzhu.mall.security.LoginUserContext;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AdminShopController 单元测试（店铺审核/等级）。
 */
class AdminShopControllerTest {

    private ShopService shopService;
    private ShopMapper shopMapper;
    private OperationLogService operationLogService;
    private AdminShopController controller;

    @BeforeAll
    static void initTableInfo() {
        if (!TableInfoHelper.getTableInfos().stream()
                .anyMatch(t -> t.getEntityType() == Shop.class)) {
            TableInfoHelper.initTableInfo(
                    new MapperBuilderAssistant(new MybatisConfiguration(), ""), Shop.class);
        }
    }

    @BeforeEach
    void setUp() {
        shopService = mock(ShopService.class);
        shopMapper = mock(ShopMapper.class);
        operationLogService = mock(OperationLogService.class);
        controller = new AdminShopController();
        inject(controller, "shopService", shopService);
        inject(controller, "shopMapper", shopMapper);
        inject(controller, "operationLogService", operationLogService);
        LoginUserContext.set(1L, 3);
    }

    @AfterEach
    void tearDown() {
        LoginUserContext.clear();
    }

    @Test
    void audit_delegatesToShopService() {
        // AD-01 联动：审核通过后 ShopService 内部升级商家角色
        AdminShopController.AuditDTO dto = new AdminShopController.AuditDTO();
        dto.setApproved(true);

        assertDoesNotThrow(() -> controller.audit(1L, dto));

        verify(shopService).audit(1L, true, null);
    }

    @Test
    void updateLevel_succeeds() {
        Shop shop = new Shop();
        shop.setId(1L);
        shop.setLevel(1);
        when(shopMapper.selectById(1L)).thenReturn(shop);

        AdminShopController.LevelDTO dto = new AdminShopController.LevelDTO();
        dto.setLevel(2);

        controller.updateLevel(1L, dto);

        assertEquals(2, shop.getLevel());
        verify(shopMapper).updateById(shop);
    }

    @Test
    void updateLevel_shopNotFound_throws() {
        when(shopMapper.selectById(1L)).thenReturn(null);

        AdminShopController.LevelDTO dto = new AdminShopController.LevelDTO();
        dto.setLevel(2);

        assertThrows(BusinessException.class, () -> controller.updateLevel(1L, dto));
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
