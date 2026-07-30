package com.pzhu.mall.security;

import com.pzhu.mall.common.config.RedisKeyPrefix;
import com.pzhu.mall.modules.user.entity.User;
import com.pzhu.mall.modules.user.mapper.UserMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * H-2 修复：账号活跃状态检查器。
 *
 * <p>原实现中 JwtInterceptor 仅校验 JWT 签名与过期时间，不检查账号状态。
 * 管理员禁用账号后，该用户持有的旧 token 在最长 7 天（jwt.expire-seconds）内
 * 仍可正常访问所有受保护接口。本组件在每次鉴权时校验账号是否仍处于正常状态，
 * 使"禁用即失效"成立。</p>
 *
 * <p>性能设计：账号状态写入短 TTL（5 分钟）Redis 缓存，命中时不查库，
 * 降低高并发下对 user 表的压力；管理员禁用/启用用户时主动 {@link #evict(Long)}
 * 清除缓存，保证状态变更立即生效，无需等待缓存自然过期。</p>
 *
 * <p>降级策略：Redis 不可用时直接查库，保证功能正确性优先于性能。</p>
 */
@Service
public class AccountStatusService {

    /** 状态缓存 TTL：5 分钟（缓存失效兜底窗口，主动 evict 可立即生效） */
    private static final long CACHE_SECONDS = 300;

    @Resource
    private UserMapper userMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private String key(Long userId) {
        return RedisKeyPrefix.USER + ":status:" + userId;
    }

    /**
     * 判断账号是否处于正常可用状态（status=1 且未被逻辑删除）。
     *
     * @return true 正常；false 已禁用/已删除/不存在
     */
    public boolean isActive(Long userId) {
        if (userId == null) {
            return false;
        }
        String k = key(userId);
        // 1. 优先读缓存
        try {
            String cached = stringRedisTemplate.opsForValue().get(k);
            if (cached != null) {
                return "1".equals(cached);
            }
        } catch (Exception e) {
            // Redis 不可用：降级查库
        }
        // 2. 查库（MyBatis-Plus 逻辑删除会自动过滤 is_deleted=1 的记录）
        User user = userMapper.selectById(userId);
        boolean active = user != null && Integer.valueOf(1).equals(user.getStatus());
        // 3. 回填缓存（失败不影响本次判定）
        try {
            stringRedisTemplate.opsForValue().set(k, active ? "1" : "0", CACHE_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            // 缓存写失败：下次重新查库即可
        }
        return active;
    }

    /**
     * H-2 修复：账号状态变更（管理员禁用/启用）时清除缓存，使变更立即生效。
     */
    public void evict(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            stringRedisTemplate.delete(key(userId));
        } catch (Exception e) {
            // 清除失败：最坏情况下 5 分钟后缓存自然过期
        }
    }
}
