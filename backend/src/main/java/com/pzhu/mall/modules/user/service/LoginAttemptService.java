package com.pzhu.mall.modules.user.service;

import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.common.config.RedisKeyPrefix;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 登录暴力破解防护。
 * <p>同一用户名连续失败 5 次后锁定 15 分钟。</p>
 */
@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_SECONDS = 15 * 60; // 15 分钟

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public void recordFailure(String username) {
        String key = RedisKeyPrefix.LOGIN_FAIL + ":" + username;
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count == 1) {
            // 首次失败，设置过期时间
            stringRedisTemplate.expire(key, LOCK_SECONDS, TimeUnit.SECONDS);
        }
    }

    public void clear(String username) {
        String key = RedisKeyPrefix.LOGIN_FAIL + ":" + username;
        stringRedisTemplate.delete(key);
    }

    public void checkAllowed(String username) {
        String key = RedisKeyPrefix.LOGIN_FAIL + ":" + username;
        String val = stringRedisTemplate.opsForValue().get(key);
        if (val == null) return;
        int count;
        try {
            count = Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return;
        }
        if (count >= MAX_ATTEMPTS) {
            throw new BusinessException(ErrorCode.SYSTEM_BUSY,
                    "登录失败次数过多，请 " + LOCK_SECONDS / 60 + " 分钟后再试");
        }
    }
}
