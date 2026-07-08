package com.pzhu.mall.modules.user.service;

import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.common.config.RedisKeyPrefix;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
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

    /** Lua 脚本：原子性地自增计数并在首次创建时设置过期时间 */
    private static final DefaultRedisScript<Long> INCR_EXPIRE_SCRIPT = new DefaultRedisScript<>(
        "local val = redis.call('INCR', KEYS[1]); " +
        "if val == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end; " +
        "return val;",
        Long.class
    );

    public void recordFailure(String username) {
        String key = RedisKeyPrefix.LOGIN_FAIL + ":" + username;
        stringRedisTemplate.execute(INCR_EXPIRE_SCRIPT, Collections.singletonList(key), String.valueOf(LOCK_SECONDS));
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
