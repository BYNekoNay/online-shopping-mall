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

    // ==================== apply（入驻申请） ====================

    @Test
    void apply_nullDto_throws() {
        org.junit.jupiter.api.Assertions.assertThrows(
                com.pzhu.mall.common.exception.BusinessException.class,
                () -> service.apply(100L, null));
    }

    @Test
    void apply_firstApplication_insertsNewShop() {
        when(shopMapper.selectOne(any())).thenReturn(null);

        com.pzhu.mall.modules.shop.dto.ShopApplyDTO dto = new com.pzhu.mall.modules.shop.dto.ShopApplyDTO();
        dto.setName("测试店铺");
        dto.setContactName("张三");
        dto.setContactPhone("13800000000");
        dto.setLicenseNo("LIC001");
        var vo = service.apply(100L, dto);

        org.junit.jupiter.api.Assertions.assertEquals(0, vo.getStatus());
        var captor = org.mockito.ArgumentCaptor.forClass(Shop.class);
        verify(shopMapper).insert(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(100L, captor.getValue().getMerchantUserId());
        org.junit.jupiter.api.Assertions.assertEquals("测试店铺", captor.getValue().getName());
    }

    @Test
    void apply_existingPending_throws() {
        // 已有待审核记录，不允许重复申请
        Shop exist = new Shop();
        exist.setId(1L);
        exist.setMerchantUserId(100L);
        exist.setStatus(0);
        when(shopMapper.selectOne(any())).thenReturn(exist);

        com.pzhu.mall.modules.shop.dto.ShopApplyDTO dto = new com.pzhu.mall.modules.shop.dto.ShopApplyDTO();
        dto.setName("重复申请");

        org.junit.jupiter.api.Assertions.assertThrows(
                com.pzhu.mall.common.exception.BusinessException.class,
                () -> service.apply(100L, dto));
    }

    @Test
    void apply_rejected_ressubmitReusesRecord() {
        // SH-03 修复验证：拒绝(status=2)后可重新提交，复用同一条记录并重置状态
        Shop exist = new Shop();
        exist.setId(1L);
        exist.setMerchantUserId(100L);
        exist.setStatus(2);
        exist.setRejectReason("资料不全");
        when(shopMapper.selectOne(any())).thenReturn(exist);

        com.pzhu.mall.modules.shop.dto.ShopApplyDTO dto = new com.pzhu.mall.modules.shop.dto.ShopApplyDTO();
        dto.setName("重新申请");
        dto.setContactName("张三");
        dto.setContactPhone("13800000000");
        dto.setLicenseNo("LIC002");

        var vo = service.apply(100L, dto);

        org.junit.jupiter.api.Assertions.assertEquals(0, vo.getStatus());
        org.junit.jupiter.api.Assertions.assertNull(exist.getRejectReason());
        verify(shopMapper, never()).insert(any());
        verify(shopMapper).updateById(exist);
    }

    @Test
    void applyStatus_noRecord_returnsEmptyVO() {
        when(shopMapper.selectOne(any())).thenReturn(null);

        var vo = service.applyStatus(100L);

        org.junit.jupiter.api.Assertions.assertNotNull(vo);
        org.junit.jupiter.api.Assertions.assertNull(vo.getStatus());
    }
}
