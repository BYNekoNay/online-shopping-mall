package com.pzhu.mall.security;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final AccountStatusService accountStatusService;

    private static final String[] WHITE_LIST = {
            "/api/auth/register",
            "/api/auth/login",
            "/api/products",
            "/api/products/search",
            "/api/products/",
            "/api/recommend/guess-you-like",
            "/api/recommend/similar/",
            "/api/promotions/active",
            "/api/behavior/page-view",
            "/api/behavior/page-view/"
    };

    public JwtInterceptor(JwtUtil jwtUtil, AccountStatusService accountStatusService) {
        this.jwtUtil = jwtUtil;
        this.accountStatusService = accountStatusService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();

        try {
            if (isWhiteListed(uri)) {
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    try {
                        if (!jwtUtil.isExpired(token)) {
                            var claims = jwtUtil.parseToken(token);
                            Long userId = Long.parseLong(claims.getSubject());
                            Integer role = claims.get("role", Integer.class);
                            // H-2 修复：白名单路径为可选登录，账号已禁用/注销时按匿名处理（不写入上下文）
                            if (accountStatusService.isActive(userId)) {
                                LoginUserContext.set(userId, role);
                            }
                        }
                    } catch (Exception e) {
                        // optional login failed, continue as anonymous
                    }
                }
                return true;
            }

            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new com.pzhu.mall.common.exception.BusinessException(com.pzhu.mall.common.enums.ErrorCode.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);
            try {
                if (jwtUtil.isExpired(token)) {
                    throw new com.pzhu.mall.common.exception.BusinessException(com.pzhu.mall.common.enums.ErrorCode.UNAUTHORIZED);
                }
            } catch (IllegalArgumentException e) {
                // isExpired() 对签名/格式无效 Token 抛出 IllegalArgumentException，统一转为 UNAUTHORIZED
                throw new com.pzhu.mall.common.exception.BusinessException(com.pzhu.mall.common.enums.ErrorCode.UNAUTHORIZED);
            }

            try {
                var claims = jwtUtil.parseToken(token);
                Long userId = Long.parseLong(claims.getSubject());
                Integer role = claims.get("role", Integer.class);
                // H-2 修复：受保护路径校验账号状态，禁用账号的旧 token 立即失效（不再等到 7 天过期）
                if (!accountStatusService.isActive(userId)) {
                    throw new com.pzhu.mall.common.exception.BusinessException(com.pzhu.mall.common.enums.ErrorCode.ACCOUNT_DISABLED);
                }
                LoginUserContext.set(userId, role);
            } catch (com.pzhu.mall.common.exception.BusinessException e) {
                throw e;
            } catch (Exception e) {
                throw new com.pzhu.mall.common.exception.BusinessException(com.pzhu.mall.common.enums.ErrorCode.UNAUTHORIZED);
            }

            return true;
        } catch (Exception e) {
            // Ensure cleanup before rethrowing to prevent ThreadLocal leaks
            LoginUserContext.clear();
            throw e;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        LoginUserContext.clear();
    }

    private boolean isWhiteListed(String uri) {
        for (String pattern : WHITE_LIST) {
            if (pattern.endsWith("/")) {
                // 前缀匹配时，确保后续路径不是 admin/ 等受保护路径（M1 修复：toLowerCase 防大小写绕过）
                if (uri.startsWith(pattern)) {
                    String remaining = uri.substring(pattern.length());
                    if (!remaining.isEmpty() && !remaining.toLowerCase().startsWith("admin")) {
                        return true;
                    }
                }
            } else if (uri.equals(pattern)) {
                return true;
            }
        }
        return false;
    }
}
