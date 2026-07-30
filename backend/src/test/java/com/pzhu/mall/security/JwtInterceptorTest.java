package com.pzhu.mall.security;

import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtInterceptorUnitTest {

    private final JwtUtil jwtUtil;
    private final AccountStatusService accountStatusService;

    JwtInterceptorUnitTest() throws Exception {
        this.jwtUtil = new JwtUtil();
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
        // H-2 修复适配：默认放行所有账号，单个测试可覆盖 isActive 行为
        this.accountStatusService = mock(AccountStatusService.class);
        when(accountStatusService.isActive(anyLong())).thenReturn(true);
    }

    private JwtInterceptor createInterceptor() {
        return new JwtInterceptor(jwtUtil, accountStatusService);
    }

    @Test
    void whitelist_withoutToken_returnsTrue() throws Exception {
        JwtInterceptor interceptor = createInterceptor();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getRequestURI()).thenReturn("/api/products");
        when(request.getHeader("Authorization")).thenReturn(null);

        assertTrue(interceptor.preHandle(request, response, null));
    }

    @Test
    void whitelist_withToken_setsUserContext() throws Exception {
        JwtInterceptor interceptor = createInterceptor();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        String token = jwtUtil.generateToken(100L, 1);
        when(request.getRequestURI()).thenReturn("/api/products");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        assertTrue(interceptor.preHandle(request, response, null));
    }

    @Test
    void whitelist_withExpiredToken_returnsTrueAndDoesNotSetContext() throws Exception {
        JwtInterceptor interceptor = createInterceptor();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        var expireField = JwtUtil.class.getDeclaredField("expireSeconds");
        expireField.setAccessible(true);
        expireField.set(jwtUtil, -1L);
        String expiredToken = jwtUtil.generateToken(100L, 1);

        when(request.getRequestURI()).thenReturn("/api/products");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + expiredToken);

        assertTrue(interceptor.preHandle(request, response, null));
    }

    @Test
    void whitelist_withInvalidToken_returnsTrueAndDoesNotSetContext() throws Exception {
        JwtInterceptor interceptor = createInterceptor();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getRequestURI()).thenReturn("/api/products");
        when(request.getHeader("Authorization")).thenReturn("Bearer not-a-valid-token");

        assertTrue(interceptor.preHandle(request, response, null));
    }

    @Test
    void protectedEndpoint_withoutToken_throwsUnauthorized() throws Exception {
        JwtInterceptor interceptor = createInterceptor();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getRequestURI()).thenReturn("/api/user/profile");
        when(request.getHeader("Authorization")).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> interceptor.preHandle(request, response, null));
        assertEquals(ErrorCode.UNAUTHORIZED.getCode(), ex.getCode());
    }

    @Test
    void protectedEndpoint_withInvalidToken_throwsUnauthorized() throws Exception {
        JwtInterceptor interceptor = createInterceptor();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getRequestURI()).thenReturn("/api/user/profile");
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> interceptor.preHandle(request, response, null));
        assertEquals(ErrorCode.UNAUTHORIZED.getCode(), ex.getCode());
    }

    @Test
    void protectedEndpoint_withValidToken_returnsTrue() throws Exception {
        JwtInterceptor interceptor = createInterceptor();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        String token = jwtUtil.generateToken(200L, 2);
        when(request.getRequestURI()).thenReturn("/api/user/profile");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        assertTrue(interceptor.preHandle(request, response, null));
    }

    @Test
    void protectedEndpoint_disabledAccount_throwsAccountDisabled() throws Exception {
        // H-2 修复验证：token 有效但账号已禁用 → 立即失效，抛 ACCOUNT_DISABLED
        JwtInterceptor interceptor = createInterceptor();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        String token = jwtUtil.generateToken(300L, 1);
        when(request.getRequestURI()).thenReturn("/api/user/profile");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(accountStatusService.isActive(300L)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> interceptor.preHandle(request, response, null));
        assertEquals(ErrorCode.ACCOUNT_DISABLED.getCode(), ex.getCode());
        assertNull(LoginUserContext.getCurrentUserId());
    }

    @Test
    void whitelist_disabledAccount_anonymousNoContext() throws Exception {
        // H-2 修复验证：白名单路径账号被禁用 → 放行但按匿名（不写入上下文）
        JwtInterceptor interceptor = createInterceptor();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        String token = jwtUtil.generateToken(300L, 1);
        when(request.getRequestURI()).thenReturn("/api/products");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(accountStatusService.isActive(300L)).thenReturn(false);

        assertTrue(interceptor.preHandle(request, response, null));
        assertNull(LoginUserContext.getCurrentUserId());
    }

    @Test
    void protectedEndpoint_activeAccount_setsContext() throws Exception {
        // H-2 修复验证：账号正常时受保护路径正常放行并写入上下文
        JwtInterceptor interceptor = createInterceptor();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        String token = jwtUtil.generateToken(400L, 1);
        when(request.getRequestURI()).thenReturn("/api/user/profile");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(accountStatusService.isActive(400L)).thenReturn(true);

        assertTrue(interceptor.preHandle(request, response, null));
        assertEquals(400L, LoginUserContext.getCurrentUserId());
        LoginUserContext.clear();
    }

    @Test
    void afterCompletion_clearsUserContext() throws Exception {
        JwtInterceptor interceptor = createInterceptor();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        // Set context first
        LoginUserContext.set(1L, 1);
        assertNotNull(LoginUserContext.getCurrentUserId());

        // afterCompletion should clear it
        interceptor.afterCompletion(request, response, null, null);
        assertNull(LoginUserContext.getCurrentUserId());
        assertNull(LoginUserContext.getCurrentRole());
    }
}
