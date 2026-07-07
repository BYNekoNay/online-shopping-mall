package com.pzhu.mall.security;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    private static final String[] WHITE_LIST = {
            "/api/auth/register",
            "/api/auth/login",
            "/api/products",
            "/api/products/search",
            "/api/products/",
            "/api/recommend/guess-you-like",
            "/api/recommend/similar/",
            "/api/promotions/active",
            "/api/upload/image",
            "/api/behavior/page-view"
    };

    public JwtInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();

        if (isWhiteListed(uri)) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    if (!jwtUtil.isExpired(token)) {
                        var claims = jwtUtil.parseToken(token);
                        Long userId = Long.parseLong(claims.getSubject());
                        Integer role = claims.get("role", Integer.class);
                        LoginUserContext.set(userId, role);
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
        if (jwtUtil.isExpired(token)) {
            throw new com.pzhu.mall.common.exception.BusinessException(com.pzhu.mall.common.enums.ErrorCode.UNAUTHORIZED);
        }

        try {
            var claims = jwtUtil.parseToken(token);
            Long userId = Long.parseLong(claims.getSubject());
            Integer role = claims.get("role", Integer.class);
            LoginUserContext.set(userId, role);
        } catch (Exception e) {
            throw new com.pzhu.mall.common.exception.BusinessException(com.pzhu.mall.common.enums.ErrorCode.UNAUTHORIZED);
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        LoginUserContext.clear();
    }

    private boolean isWhiteListed(String uri) {
        for (String pattern : WHITE_LIST) {
            if (pattern.endsWith("/") && uri.startsWith(pattern)) return true;
            if (uri.equals(pattern)) return true;
        }
        return false;
    }
}
