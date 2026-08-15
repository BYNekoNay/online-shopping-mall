package com.pzhu.mall.modules.admin.controller;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.common.result.PageResult;
import com.pzhu.mall.modules.admin.service.OperationLogService;
import com.pzhu.mall.modules.admin.vo.AdminUserVO;
import com.pzhu.mall.modules.user.entity.User;
import com.pzhu.mall.modules.user.mapper.UserMapper;
import com.pzhu.mall.security.AccountStatusService;
import com.pzhu.mall.security.LoginUserContext;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * AdminUserController 单元测试（用户管理：列表/禁用/角色分配 AD-01）。
 * <p>覆盖 docs/32 批次3 的 A-T01~04 用例。</p>
 */
class AdminUserControllerTest {

    private UserMapper userMapper;
    private OperationLogService operationLogService;
    private AccountStatusService accountStatusService;
    private com.pzhu.mall.modules.order.mapper.OrderMapper orderMapper;
    private com.pzhu.mall.modules.behavior.mapper.UserBehaviorMapper userBehaviorMapper;
    private AdminUserController controller;

    @BeforeAll
    static void initTableInfo() {
        if (!TableInfoHelper.getTableInfos().stream()
                .anyMatch(t -> t.getEntityType() == User.class)) {
            TableInfoHelper.initTableInfo(
                    new MapperBuilderAssistant(new MybatisConfiguration(), ""), User.class);
        }
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                com.pzhu.mall.modules.order.entity.Order.class);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                com.pzhu.mall.modules.behavior.entity.UserBehavior.class);
    }

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        operationLogService = mock(OperationLogService.class);
        accountStatusService = mock(AccountStatusService.class);
        orderMapper = mock(com.pzhu.mall.modules.order.mapper.OrderMapper.class);
        userBehaviorMapper = mock(com.pzhu.mall.modules.behavior.mapper.UserBehaviorMapper.class);
        controller = new AdminUserController();
        inject(controller, "userMapper", userMapper);
        inject(controller, "operationLogService", operationLogService);
        inject(controller, "accountStatusService", accountStatusService);
        inject(controller, "orderMapper", orderMapper);
        inject(controller, "userBehaviorMapper", userBehaviorMapper);
    }

    @AfterEach
    void tearDown() {
        LoginUserContext.clear();
    }

    @Test
    void list_returnsPagedUsers() {
        Page<User> page = new Page<>(1, 10);
        page.setRecords(Collections.singletonList(user(1L, "user1", 1, 1)));
        page.setTotal(1);
        when(userMapper.selectPage(any(Page.class), any())).thenReturn(page);

        PageResult<AdminUserVO> result = controller.list(1, 10, null, null, null).getData();

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
    }

    @Test
    void updateStatus_disableOwnAccount_throws() {
        // H-03 修复验证：不能禁用自己的账号
        LoginUserContext.set(1L, 3);
        when(userMapper.selectById(1L)).thenReturn(user(1L, "admin", 3, 1));

        AdminUserController.StatusDTO dto = new AdminUserController.StatusDTO();
        dto.setStatus(0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.updateStatus(1L, dto));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void updateStatus_disableOtherAdmin_throws() {
        // H-03 修复验证：不能禁用其他管理员
        LoginUserContext.set(100L, 3);
        when(userMapper.selectById(2L)).thenReturn(user(2L, "admin2", 3, 1));

        AdminUserController.StatusDTO dto = new AdminUserController.StatusDTO();
        dto.setStatus(0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.updateStatus(2L, dto));
        assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    void updateStatus_disableConsumer_succeeds() {
        LoginUserContext.set(100L, 3);
        User target = user(5L, "consumer1", 1, 1);
        when(userMapper.selectById(5L)).thenReturn(target);

        AdminUserController.StatusDTO dto = new AdminUserController.StatusDTO();
        dto.setStatus(0);

        controller.updateStatus(5L, dto);

        assertEquals(0, target.getStatus());
        verify(userMapper).updateById(target);
        verify(accountStatusService).evict(5L);
        verify(operationLogService).record(100L, "禁用用户", "用户#5");
    }

    @Test
    void updateStatus_userNotFound_throws() {
        LoginUserContext.set(100L, 3);
        when(userMapper.selectById(999L)).thenReturn(null);

        AdminUserController.StatusDTO dto = new AdminUserController.StatusDTO();
        dto.setStatus(0);

        assertThrows(BusinessException.class, () -> controller.updateStatus(999L, dto));
    }

    @Test
    void updateRole_assignMerchant_succeeds() {
        // AD-01 修复验证：管理员可分配商家角色
        LoginUserContext.set(100L, 3);
        User target = user(5L, "user5", 1, 1);
        when(userMapper.selectById(5L)).thenReturn(target);

        AdminUserController.RoleDTO dto = new AdminUserController.RoleDTO();
        dto.setRole(2);

        controller.updateRole(5L, dto);

        assertEquals(2, target.getRole());
        verify(userMapper).updateById(target);
        verify(accountStatusService).evict(5L);
        verify(operationLogService).record(100L, "分配用户角色", "用户#5 → 角色2");
    }

    @Test
    void updateRole_invalidRole_throws() {
        LoginUserContext.set(100L, 3);
        when(userMapper.selectById(5L)).thenReturn(user(5L, "user5", 1, 1));

        AdminUserController.RoleDTO dto = new AdminUserController.RoleDTO();
        dto.setRole(9);

        assertThrows(BusinessException.class, () -> controller.updateRole(5L, dto));
    }

    @Test
    void updateRole_demoteSelf_throws() {
        // 防自降级：管理员不能把自己的角色从 3 改走
        LoginUserContext.set(1L, 3);
        when(userMapper.selectById(1L)).thenReturn(user(1L, "admin", 3, 1));

        AdminUserController.RoleDTO dto = new AdminUserController.RoleDTO();
        dto.setRole(1);

        assertThrows(BusinessException.class, () -> controller.updateRole(1L, dto));
    }

    @Test
    void updateRole_userNotFound_throws() {
        LoginUserContext.set(100L, 3);
        when(userMapper.selectById(999L)).thenReturn(null);

        AdminUserController.RoleDTO dto = new AdminUserController.RoleDTO();
        dto.setRole(2);

        assertThrows(BusinessException.class, () -> controller.updateRole(999L, dto));
    }

    // ==================== detail（B-2 用户详情） ====================

    @Test
    void detail_userExists_returnsFullProfile() {
        // U-01：用户存在 → 返回完整字段（含订单数/消费/行为）
        when(userMapper.selectById(5L)).thenReturn(user(5L, "user5", 1, 1));
        when(orderMapper.selectCount(any())).thenReturn(3L);

        com.pzhu.mall.modules.order.entity.Order paid = new com.pzhu.mall.modules.order.entity.Order();
        paid.setPayAmount(new java.math.BigDecimal("100.50"));
        when(orderMapper.selectList(any())).thenReturn(
                java.util.List.of(paid, paid));

        var behavior = new com.pzhu.mall.modules.behavior.entity.UserBehavior();
        behavior.setUserId(5L);
        behavior.setProductId(10L);
        behavior.setBehaviorType(1);
        when(userBehaviorMapper.selectList(any())).thenReturn(java.util.List.of(behavior));

        var result = controller.detail(5L).getData();

        assertEquals("user5", result.get("username"));
        assertEquals(3L, result.get("orderCount"));
        assertEquals(new java.math.BigDecimal("201.00"), result.get("totalSpend"));
        assertEquals(1, ((java.util.List<?>) result.get("recentBehaviors")).size());
    }

    @Test
    void detail_userNotFound_throws() {
        // U-02：用户不存在 → 抛 10004
        when(userMapper.selectById(999L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> controller.detail(999L));
    }

    @Test
    void detail_userWithoutOrders_returnsZero() {
        // U-03/U-04：无订单用户 → 0 / 0.00
        when(userMapper.selectById(5L)).thenReturn(user(5L, "user5", 1, 1));
        when(orderMapper.selectCount(any())).thenReturn(0L);
        when(orderMapper.selectList(any())).thenReturn(java.util.Collections.emptyList());
        when(userBehaviorMapper.selectList(any())).thenReturn(java.util.Collections.emptyList());

        var result = controller.detail(5L).getData();

        assertEquals(0L, result.get("orderCount"));
        assertEquals(java.math.BigDecimal.ZERO, result.get("totalSpend"));
    }

    @Test
    void detail_deletedUser_throws() {
        // U-05：已软删用户 → 抛 10004
        User deleted = user(5L, "user5", 1, 1);
        deleted.setIsDeleted(1);
        when(userMapper.selectById(5L)).thenReturn(deleted);
        assertThrows(BusinessException.class, () -> controller.detail(5L));
    }

    private static User user(Long id, String username, int role, int status) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setRole(role);
        u.setStatus(status);
        u.setIsDeleted(0);
        return u;
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
