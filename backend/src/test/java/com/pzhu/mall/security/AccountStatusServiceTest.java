package com.pzhu.mall.security;

import com.pzhu.mall.modules.user.entity.User;
import com.pzhu.mall.modules.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * H-2 修复验证：AccountStatusService 账号状态检查与缓存逻辑。
 */
class AccountStatusServiceTest {

    private UserMapper userMapper;
    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOps;
    private AccountStatusService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        userMapper = mock(UserMapper.class);
        redis = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);

        service = new AccountStatusService();
        inject("userMapper", userMapper);
        inject("stringRedisTemplate", redis);
    }

    private void inject(String fieldName, Object value) throws Exception {
        var field = AccountStatusService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(service, value);
    }

    private static User user(Long id, int status) {
        User u = new User();
        u.setId(id);
        u.setStatus(status);
        return u;
    }

    @Test
    void isActive_cacheHitActive_returnsTrueWithoutDb() {
        when(valueOps.get(anyString())).thenReturn("1");
        assertTrue(service.isActive(1L));
        verify(userMapper, never()).selectById(anyLong());
    }

    @Test
    void isActive_cacheHitDisabled_returnsFalseWithoutDb() {
        when(valueOps.get(anyString())).thenReturn("0");
        assertFalse(service.isActive(1L));
        verify(userMapper, never()).selectById(anyLong());
    }

    @Test
    void isActive_cacheMiss_dbActive_cachesAndReturnsTrue() {
        when(valueOps.get(anyString())).thenReturn(null);
        when(userMapper.selectById(1L)).thenReturn(user(1L, 1));

        assertTrue(service.isActive(1L));
        verify(valueOps).set(anyString(), eq("1"), anyLong(), any(TimeUnit.class));
    }

    @Test
    void isActive_cacheMiss_dbDisabled_cachesAndReturnsFalse() {
        when(valueOps.get(anyString())).thenReturn(null);
        when(userMapper.selectById(1L)).thenReturn(user(1L, 0));

        assertFalse(service.isActive(1L));
        verify(valueOps).set(anyString(), eq("0"), anyLong(), any(TimeUnit.class));
    }

    @Test
    void isActive_cacheMiss_userNotFound_returnsFalse() {
        when(valueOps.get(anyString())).thenReturn(null);
        when(userMapper.selectById(1L)).thenReturn(null);

        assertFalse(service.isActive(1L));
    }

    @Test
    void isActive_nullUserId_returnsFalse() {
        assertFalse(service.isActive(null));
        verify(userMapper, never()).selectById(anyLong());
    }

    @Test
    void isActive_redisDown_fallsBackToDb() {
        // Redis 读异常时降级查库，功能正确性优先
        when(valueOps.get(anyString())).thenThrow(new RuntimeException("redis down"));
        when(userMapper.selectById(1L)).thenReturn(user(1L, 1));

        assertTrue(service.isActive(1L));
    }

    @Test
    void evict_deletesCacheKey() {
        service.evict(1L);
        verify(redis).delete(anyString());
    }
}
