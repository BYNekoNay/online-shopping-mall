package com.pzhu.mall.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 安全响应头过滤器。
 * <p>为所有响应添加基础安全头，防止常见 Web 攻击。</p>
 */
@Configuration
public class SecurityHeadersFilter {

    @Bean(name = "securityHeadersFilterBean")
    public FilterRegistrationBean<SecurityHeadersFilter.HeaderFilter> securityHeadersFilter() {
        FilterRegistrationBean<SecurityHeadersFilter.HeaderFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new HeaderFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    public static class HeaderFilter implements Filter {
        @Override
        public void doFilter(ServletRequest request, ServletResponse response,
                             FilterChain chain) throws IOException, ServletException {
            HttpServletResponse httpResp = (HttpServletResponse) response;
            httpResp.setHeader("X-Content-Type-Options", "nosniff");
            httpResp.setHeader("X-Frame-Options", "DENY");
            httpResp.setHeader("X-XSS-Protection", "1; mode=block");
            chain.doFilter(request, response);
        }
    }
}
