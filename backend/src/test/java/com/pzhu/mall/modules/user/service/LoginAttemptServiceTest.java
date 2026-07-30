package com.pzhu.mall.modules.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * H-1 修复验证：LoginAttemptService 客户端 IP 解析的可信代理逻辑。
 *
 * <p>核心安全约束：仅当请求直连来源 remoteAddr 属于配置的可信反向代理时，
 * 才读取 X-Forwarded-For / X-Real-IP；否则一律使用 remoteAddr，
 * 防止攻击者伪造代理头绕过"用户名 + IP"登录限流或伪造受害者 IP 制造锁定 DoS。
 */
class LoginAttemptServiceTest {

    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        service = new LoginAttemptService();
    }

    private void setTrustedProxies(String value) {
        try {
            var field = LoginAttemptService.class.getDeclaredField("trustedProxies");
            field.setAccessible(true);
            field.set(service, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static HttpServletRequest request(String remoteAddr, String xff, String xRealIp) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRemoteAddr()).thenReturn(remoteAddr);
        when(req.getHeader("X-Forwarded-For")).thenReturn(xff);
        when(req.getHeader("X-Real-IP")).thenReturn(xRealIp);
        return req;
    }

    @Test
    void resolveClientIp_noTrustedProxyConfig_ignoresForwardedHeaders() {
        // 默认（空配置）不信任任何代理：即便携带 XFF 也只认 remoteAddr
        setTrustedProxies("");
        HttpServletRequest req = request("1.2.3.4", "9.9.9.9", "8.8.8.8");
        assertEquals("1.2.3.4", service.resolveClientIp(req));
    }

    @Test
    void resolveClientIp_untrustedSource_ignoresForwardedHeaders() {
        // 配置了可信代理但直连来源不在列表内：代理头不可信
        setTrustedProxies("127.0.0.1");
        HttpServletRequest req = request("5.6.7.8", "9.9.9.9", "8.8.8.8");
        assertEquals("5.6.7.8", service.resolveClientIp(req));
    }

    @Test
    void resolveClientIp_trustedProxy_readsXForwardedForFirst() {
        // 直连来源是可信代理：取 XFF 列表第一个真实 IP
        setTrustedProxies("127.0.0.1");
        HttpServletRequest req = request("127.0.0.1", "9.9.9.9, 8.8.8.8", null);
        assertEquals("9.9.9.9", service.resolveClientIp(req));
    }

    @Test
    void resolveClientIp_trustedProxy_fallsBackToXRealIp() {
        // 无 XFF 时回退 X-Real-IP
        setTrustedProxies("127.0.0.1");
        HttpServletRequest req = request("127.0.0.1", null, "7.7.7.7");
        assertEquals("7.7.7.7", service.resolveClientIp(req));
    }

    @Test
    void resolveClientIp_trustedProxy_noHeaders_usesRemoteAddr() {
        // 可信代理但无任何代理头：回退 remoteAddr
        setTrustedProxies("127.0.0.1");
        HttpServletRequest req = request("127.0.0.1", null, null);
        assertEquals("127.0.0.1", service.resolveClientIp(req));
    }

    @Test
    void resolveClientIp_cidrMatch_trustsProxy() {
        // CIDR 命中：10.0.0.0/8 信任 10.1.2.3
        setTrustedProxies("10.0.0.0/8");
        HttpServletRequest req = request("10.1.2.3", "9.9.9.9", null);
        assertEquals("9.9.9.9", service.resolveClientIp(req));
    }

    @Test
    void resolveClientIp_cidrNoMatch_usesRemoteAddr() {
        // CIDR 未命中：11.x 不在 10.0.0.0/8 内
        setTrustedProxies("10.0.0.0/8");
        HttpServletRequest req = request("11.1.2.3", "9.9.9.9", null);
        assertEquals("11.1.2.3", service.resolveClientIp(req));
    }

    @Test
    void resolveClientIp_multipleEntries_matchesAny() {
        // 多条目任一命中即可信
        setTrustedProxies("127.0.0.1, 10.0.0.0/8");
        HttpServletRequest req = request("10.9.9.9", "6.6.6.6", null);
        assertEquals("6.6.6.6", service.resolveClientIp(req));
    }
}
