package com.pzhu.mall.security;

import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private final JwtUtil jwtUtil = new JwtUtil();

    public JwtUtilTest() throws Exception {
        var field = JwtUtil.class.getDeclaredField("secret");
        field.setAccessible(true);
        field.set(jwtUtil, "test-secret-key-for-unit-test-only-1234567890");
        var expireField = JwtUtil.class.getDeclaredField("expireSeconds");
        expireField.setAccessible(true);
        expireField.set(jwtUtil, 3600L);
        var keyField = JwtUtil.class.getDeclaredField("key");
        keyField.setAccessible(true);
        keyField.set(jwtUtil, io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                "test-secret-key-for-unit-test-only-1234567890".getBytes()));
    }

    @Test
    void generateAndParseToken_success() {
        String token = jwtUtil.generateToken(100L, 1);
        assertNotNull(token);
        assertFalse(token.isEmpty());

        var claims = jwtUtil.parseToken(token);
        assertEquals("100", claims.getSubject());
        assertEquals(1, claims.get("role", Integer.class));
    }

    @Test
    void parseToken_expired_returnsTrue() throws Exception {
        var field = JwtUtil.class.getDeclaredField("expireSeconds");
        field.setAccessible(true);
        field.set(jwtUtil, -1L);

        String token = jwtUtil.generateToken(100L, 1);

        var keyField = JwtUtil.class.getDeclaredField("key");
        keyField.setAccessible(true);
        keyField.set(jwtUtil, io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                "test-secret-key-for-unit-test-only-1234567890".getBytes()));

        assertTrue(jwtUtil.isExpired(token));
    }

    @Test
    void isExpired_malformedToken_throwsIllegalArgument() {
        // M2 修复后：格式错误/签名无效的 token 不再视为"过期"，而是抛出异常让调用方区分
        assertThrows(IllegalArgumentException.class, () -> jwtUtil.isExpired("not-a-valid-token"));
    }
}
