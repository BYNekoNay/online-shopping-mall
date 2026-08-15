package com.pzhu.mall.modules.user.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.modules.user.entity.User;
import com.pzhu.mall.modules.user.mapper.UserMapper;
import com.pzhu.mall.security.JwtUtil;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * UserService 单元测试（注册唯一性 / 密码强度 / 登录）。
 * <p>覆盖 docs/32 批次1 的 U-T07~09 用例。</p>
 */
class UserServiceTest {

    private UserMapper userMapper;
    private JwtUtil jwtUtil;
    private LoginAttemptService loginAttemptService;
    private UserService service;

    @BeforeAll
    static void initTableInfo() {
        if (!TableInfoHelper.getTableInfos().stream()
                .anyMatch(t -> t.getEntityType() == User.class)) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), User.class);
        }
    }

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        jwtUtil = mock(JwtUtil.class);
        loginAttemptService = mock(LoginAttemptService.class);
        service = new UserService(userMapper, jwtUtil, loginAttemptService);
    }

    // ==================== register ====================

    @Test
    void register_passwordTooShort_throws() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.register("user1", "1234567", "昵称", null, null));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("8 位"));
    }

    @Test
    void register_usernameExists_throws() {
        when(userMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.register("user1", "12345678", "昵称", null, null));
        assertEquals(ErrorCode.USERNAME_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void register_phoneExists_throws() {
        // 用户名唯一通过，手机号冲突 → 业务异常（U-01 回归）
        when(userMapper.selectCount(any()))
                .thenReturn(0L)  // username
                .thenReturn(1L); // phone
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.register("user1", "12345678", "昵称", "13800000000", null));
        assertTrue(ex.getMessage().contains("手机号"));
    }

    @Test
    void register_emailExists_throws() {
        // phone 为 null 时只查 username 与 email（两次 selectCount）
        when(userMapper.selectCount(any()))
                .thenReturn(0L)  // username
                .thenReturn(1L); // email
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.register("user1", "12345678", "昵称", null, "a@b.com"));
        assertTrue(ex.getMessage().contains("邮箱"));
    }

    @Test
    void register_success_insertsEncodedPassword() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any())).thenReturn(1);

        service.register("user1", "12345678", "昵称", null, null);

        // 密码 BCrypt 加密后入库
        var captor = org.mockito.ArgumentCaptor.forClass(com.pzhu.mall.modules.user.entity.User.class);
        verify(userMapper).insert(captor.capture());
        assertNotEquals("12345678", captor.getValue().getPassword());
        assertTrue(new BCryptPasswordEncoder().matches("12345678", captor.getValue().getPassword()));
        assertEquals(Integer.valueOf(1), captor.getValue().getRole());
        assertEquals(Integer.valueOf(1), captor.getValue().getStatus());
    }

    @Test
    void register_duplicateKeyException_mapsToExists() {
        // 预查与唯一索引双保险：并发插入撞唯一键 → 按异常消息归类，不抛 500
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any())).thenThrow(new org.springframework.dao.DuplicateKeyException("uk_username"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.register("user1", "12345678", "昵称", null, null));
        assertEquals(ErrorCode.USERNAME_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void register_duplicateKey_unknownMessage_fallsBackToParamError() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any())).thenThrow(new org.springframework.dao.DuplicateKeyException("generic"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.register("user1", "12345678", "昵称", null, null));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    // ==================== login ====================

    @Test
    void login_success_returnsToken() {
        com.pzhu.mall.modules.user.entity.User user = new com.pzhu.mall.modules.user.entity.User();
        user.setId(1L);
        user.setUsername("user1");
        user.setPassword(new BCryptPasswordEncoder().encode("12345678"));
        user.setStatus(1);
        user.setRole(1);
        user.setIsDeleted(0);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(jwtUtil.generateToken(anyLong(), anyInt())).thenReturn("token-abc");

        String token = service.login("user1", "12345678");

        assertEquals("token-abc", token);
        verify(loginAttemptService).clear(anyString());
        verify(loginAttemptService).checkAllowed(anyString());
    }

    @Test
    void login_wrongPassword_throwsAndRecordsFailure() {
        com.pzhu.mall.modules.user.entity.User user = new com.pzhu.mall.modules.user.entity.User();
        user.setId(1L);
        user.setUsername("user1");
        user.setPassword(new BCryptPasswordEncoder().encode("correct-pw"));
        user.setStatus(1);
        user.setIsDeleted(0);
        when(userMapper.selectOne(any())).thenReturn(user);

        assertThrows(BusinessException.class, () -> service.login("user1", "wrong-pw"));
        verify(loginAttemptService).recordFailure(anyString());
        verify(loginAttemptService).checkAllowed(anyString());
    }

    @Test
    void login_userDisabled_throws() {
        com.pzhu.mall.modules.user.entity.User user = new com.pzhu.mall.modules.user.entity.User();
        user.setId(1L);
        user.setUsername("user1");
        user.setPassword(new BCryptPasswordEncoder().encode("12345678"));
        user.setStatus(0); // 禁用
        user.setIsDeleted(0);
        when(userMapper.selectOne(any())).thenReturn(user);

        assertThrows(BusinessException.class, () -> service.login("user1", "12345678"));
    }
}
