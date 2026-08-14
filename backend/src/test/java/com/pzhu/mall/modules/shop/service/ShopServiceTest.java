package com.pzhu.mall.modules.shop.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.pzhu.mall.modules.shop.entity.Shop;
import com.pzhu.mall.modules.shop.mapper.ShopMapper;
import com.pzhu.mall.modules.user.entity.User;
import com.pzhu.mall.modules.user.mapper.UserMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * AD-01 修复验证：入驻审核通过后联动升级用户角色为商家（role=2）。
 */
class ShopServiceTest {

    private ShopMapper shopMapper;
    private UserMapper userMapper;
    private ShopService service;

    @BeforeAll
    static void initTableInfo() {
        var assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Shop.class);
        TableInfoHelper.initTableInfo(assistant, User.class);
    }

    @BeforeEach
    void setUp() {
        shopMapper = mock(ShopMapper.class);
        userMapper = mock(UserMapper.class);
        service = new ShopService(shopMapper, userMapper);
    }

    @Test
    void auditApproved_shouldUpgradeUserRoleToMerchant() {
        Shop shop = new Shop();
        shop.setId(1L);
        shop.setMerchantUserId(100L);
        shop.setStatus(0);
        when(shopMapper.selectById(1L)).thenReturn(shop);
        when(shopMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        service.audit(1L, true, null);

        // 审核通过：必须触发 user.role=2 的联动更新
        verify(userMapper).update(eq(null), any(LambdaUpdateWrapper.class));
        verify(shopMapper).update(any(), any(LambdaUpdateWrapper.class));
    }

    @Test
    void auditRejected_shouldNotTouchUserRole() {
        Shop shop = new Shop();
        shop.setId(2L);
        shop.setMerchantUserId(200L);
        shop.setStatus(0);
        when(shopMapper.selectById(2L)).thenReturn(shop);
        when(shopMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        service.audit(2L, false, "资质不全");

        // 审核拒绝：不升级角色
        verify(userMapper, never()).update(any(), any());
    }

    @Test
    void auditNotPending_shouldThrow() {
        Shop shop = new Shop();
        shop.setId(3L);
        shop.setStatus(1);
        when(shopMapper.selectById(3L)).thenReturn(shop);
        when(shopMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(0);

        org.junit.jupiter.api.Assertions.assertThrows(
                com.pzhu.mall.common.exception.BusinessException.class,
                () -> service.audit(3L, true, null));
    }
}
