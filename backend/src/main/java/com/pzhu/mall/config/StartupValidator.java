package com.pzhu.mall.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 生产环境启动校验。
 * <p>确保关键敏感配置在生产模式下已正确设置，避免使用不安全的默认值启动。</p>
 */
@Component
public class StartupValidator {

    private final Environment env;

    public StartupValidator(Environment env) {
        this.env = env;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validate() {
        String[] activeProfiles = env.getActiveProfiles();
        boolean isProd = false;
        for (String profile : activeProfiles) {
            if ("prod".equalsIgnoreCase(profile)) {
                isProd = true;
                break;
            }
        }
        if (!isProd) return;

        // 校验 JWT_SECRET 非默认值
        String jwtSecret = env.getProperty("jwt.secret", "");
        // m1 修复：仅匹配包含 "dev" 或 "change" 等明显占位关键词的弱密钥，避免误杀合法密钥
        if (jwtSecret == null || jwtSecret.isBlank()
                || jwtSecret.contains("change-in-production")
                || (jwtSecret.length() < 16 && jwtSecret.contains("dev"))) {
            throw new IllegalStateException(
                    "生产环境必须设置 JWT_SECRET 环境变量，禁止使用默认/开发密钥");
        }

        // 校验数据库密码非默认值
        String dbPassword = env.getProperty("spring.datasource.password", "");
        if (dbPassword == null || dbPassword.isBlank()
                || dbPassword.equalsIgnoreCase("root")) {
            throw new IllegalStateException(
                    "生产环境必须设置 DB_PASSWORD 环境变量，禁止使用 root 空密码");
        }
    }
}
