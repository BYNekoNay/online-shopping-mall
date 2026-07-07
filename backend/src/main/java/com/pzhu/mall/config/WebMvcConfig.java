package com.pzhu.mall.config;

import com.pzhu.mall.security.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    @Autowired
    public WebMvcConfig(JwtInterceptor jwtInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/products",
                        "/api/products/search",
                        "/api/products/*",
                        "/api/recommend/guess-you-like",
                        "/api/recommend/similar/*",
                        "/api/promotions/active",
                        "/api/upload/image",
                        "/api/behavior/page-view",
                        "/doc.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/favicon.ico"
                );
    }
}
