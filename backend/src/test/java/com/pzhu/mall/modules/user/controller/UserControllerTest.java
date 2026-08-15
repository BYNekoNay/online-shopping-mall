package com.pzhu.mall.modules.user.controller;

import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.modules.user.dto.LoginDTO;
import com.pzhu.mall.modules.user.dto.RegisterDTO;
import com.pzhu.mall.modules.user.entity.Address;
import com.pzhu.mall.modules.user.service.AddressService;
import com.pzhu.mall.modules.user.service.UserService;
import com.pzhu.mall.modules.user.vo.LoginVO;
import com.pzhu.mall.modules.user.vo.UserProfileVO;
import com.pzhu.mall.security.LoginUserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UserController 单元测试（注册/登录/资料/地址）。
 * <p>覆盖 docs/32 批次3 的 U-T10~12 用例。</p>
 */
class UserControllerTest {

    private UserService userService;
    private AddressService addressService;
    private com.pzhu.mall.modules.user.mapper.UserMapper userMapper;
    private UserController controller;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        addressService = mock(AddressService.class);
        userMapper = mock(com.pzhu.mall.modules.user.mapper.UserMapper.class);
        controller = new UserController();
        inject(controller, "userService", userService);
        inject(controller, "addressService", addressService);
        inject(controller, "userMapper", userMapper);
    }

    @AfterEach
    void tearDown() {
        LoginUserContext.clear();
    }

    @Test
    void register_delegatesToService() {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("user1");
        dto.setPassword("12345678");
        when(userService.register(anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(1L);

        Result<Void> result = controller.register(dto);

        assertNotNull(result);
        verify(userService).register("user1", "12345678", null, null, null);
    }

    @Test
    void login_returnsToken() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("user1");
        dto.setPassword("12345678");
        when(userService.login("user1", "12345678")).thenReturn("token-1");
        com.pzhu.mall.modules.user.entity.User user = new com.pzhu.mall.modules.user.entity.User();
        user.setId(100L);
        user.setRole(1);
        user.setNickname("昵称");
        when(userMapper.selectOne(any())).thenReturn(user);

        Result<LoginVO> result = controller.login(dto);

        assertEquals("token-1", result.getData().getToken());
        assertEquals(100L, result.getData().getUserId());
        assertEquals("昵称", result.getData().getNickname());
    }

    @Test
    void profile_returnsCurrentUser() {
        LoginUserContext.set(100L, 1);
        com.pzhu.mall.modules.user.entity.User user = new com.pzhu.mall.modules.user.entity.User();
        user.setId(100L);
        user.setNickname("昵称");
        user.setUsername("user1");
        when(userService.getById(100L)).thenReturn(user);

        Result<UserProfileVO> result = controller.profile();

        assertEquals("昵称", result.getData().getNickname());
        assertEquals("user1", result.getData().getUsername());
    }

    @Test
    void addresses_returnsCurrentUserAddresses() {
        LoginUserContext.set(100L, 1);
        List<Address> list = Collections.singletonList(new Address());
        when(addressService.listByUser(100L)).thenReturn(list);

        Result<List<Address>> result = controller.addresses();

        assertEquals(1, result.getData().size());
        verify(addressService).listByUser(100L);
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
