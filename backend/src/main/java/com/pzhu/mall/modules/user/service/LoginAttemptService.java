package com.pzhu.mall.modules.user.service;

import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.common.config.RedisKeyPrefix;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.util.Collections;

/**
 * 登录暴力破解防护。
 *
 * <p>M-21 修复：限流维度由"仅用户名"改为"用户名 + IP"与"IP 汇总"双维度，
 * 避免原实现中攻击者对任意用户名恶意连续失败 5 次即可锁定该账户（账户锁定 DoS）；
 * 同时新增 IP 级汇总限流，防止单 IP 对大量用户名发起撞库攻击。
 *
 * <ul>
 *   <li>同 IP + 同用户名：连续失败 5 次锁定 15 分钟，仅影响该 IP 登录该用户名，
 *       合法用户从其他 IP 登录不受影响。</li>
 *   <li>同 IP（跨用户名汇总）：累计失败 30 次锁定该 IP 15 分钟。</li>
 * </ul>
 */
@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    /** M-21 修复：IP 级跨用户名累计失败阈值 */
    private static final int IP_MAX_ATTEMPTS = 30;
    private static final long LOCK_SECONDS = 15 * 60; // 15 分钟

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * H-1 修复：可信反向代理 IP 列表（逗号分隔，支持 CIDR）。
     * <p>仅当请求直连来源 remoteAddr 落在该列表内才信任 X-Forwarded-For/X-Real-IP；
     * 默认空字符串=不信任任何代理，一律以 remoteAddr 作为客户端 IP。
     */
    @Value("${mall.security.trusted-proxies:}")
    private String trustedProxies;

    /** Lua 脚本：原子性地自增计数并在首次创建时设置过期时间 */
    private static final DefaultRedisScript<Long> INCR_EXPIRE_SCRIPT = new DefaultRedisScript<>(
        "local val = redis.call('INCR', KEYS[1]); " +
        "if val == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end; " +
        "return val;",
        Long.class
    );

    public void recordFailure(String username) {
        String ip = getClientIp();
        // 用户名 + IP 维度：仅锁定该 IP 对该用户名的登录，不影响其他 IP
        stringRedisTemplate.execute(INCR_EXPIRE_SCRIPT,
                Collections.singletonList(userKey(username, ip)), String.valueOf(LOCK_SECONDS));
        // IP 汇总维度：防止单 IP 跨用户名撞库
        stringRedisTemplate.execute(INCR_EXPIRE_SCRIPT,
                Collections.singletonList(ipKey(ip)), String.valueOf(LOCK_SECONDS));
    }

    public void clear(String username) {
        String ip = getClientIp();
        stringRedisTemplate.delete(userKey(username, ip));
    }

    public void checkAllowed(String username) {
        String ip = getClientIp();
        // M-21 修复：先做 IP 级限流，再做"用户名 + IP"级限流
        if (count(ipKey(ip)) >= IP_MAX_ATTEMPTS) {
            throw new BusinessException(ErrorCode.SYSTEM_BUSY,
                    "当前 IP 登录尝试次数过多，请 " + LOCK_SECONDS / 60 + " 分钟后再试");
        }
        if (count(userKey(username, ip)) >= MAX_ATTEMPTS) {
            throw new BusinessException(ErrorCode.SYSTEM_BUSY,
                    "登录失败次数过多，请 " + LOCK_SECONDS / 60 + " 分钟后再试");
        }
    }

    private String userKey(String username, String ip) {
        return RedisKeyPrefix.LOGIN_FAIL + ":" + username + ":" + ip;
    }

    private String ipKey(String ip) {
        return RedisKeyPrefix.LOGIN_FAIL + ":ip:" + ip;
    }

    private int count(String key) {
        String val = stringRedisTemplate.opsForValue().get(key);
        if (val == null) {
            return 0;
        }
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 获取客户端真实 IP。非 Web 请求上下文（如异步任务）时返回 "unknown"。
     */
    private String getClientIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return "unknown";
            }
            return resolveClientIp(attrs.getRequest());
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * H-1 修复：解析客户端真实 IP（提取为包级方法便于单元测试）。
     * <p>仅当请求的直连来源 {@code remoteAddr} 属于配置的可信反向代理时，才读取
     * X-Forwarded-For / X-Real-IP 代理头；否则一律使用 remoteAddr。
     * <p>原实现无条件信任 X-Forwarded-For，攻击者每次轮换该头部即可绕过 M-21 的
     * "用户名 + IP"双键限流，反向亦可伪造受害者 IP 触发账户锁定 DoS。
     */
    String resolveClientIp(HttpServletRequest req) {
        String remoteAddr = req.getRemoteAddr();
        // 直连来源不是可信代理：代理头不可信，直接使用 remoteAddr
        if (!isTrustedProxy(remoteAddr)) {
            return normalize(remoteAddr);
        }
        String ip = req.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For 可能为逗号分隔列表，取第一个真实 IP
            int idx = ip.indexOf(',');
            return (idx > 0 ? ip.substring(0, idx) : ip).trim();
        }
        ip = req.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }
        return normalize(remoteAddr);
    }

    private static String normalize(String ip) {
        return ip == null ? "unknown" : ip.trim();
    }

    /**
     * H-1 修复：判断直连来源 IP 是否为配置的可信反向代理。
     * <p>支持精确 IP 与 CIDR（如 10.0.0.0/8）匹配；配置为空时恒返回 false（不信任任何代理）。
     */
    private boolean isTrustedProxy(String remoteAddr) {
        if (trustedProxies == null || trustedProxies.trim().isEmpty() || remoteAddr == null) {
            return false;
        }
        for (String entry : trustedProxies.split(",")) {
            String pattern = entry.trim();
            if (pattern.isEmpty()) {
                continue;
            }
            try {
                if (pattern.contains("/")) {
                    if (matchesCidr(remoteAddr, pattern)) {
                        return true;
                    }
                } else if (pattern.equals(remoteAddr)) {
                    return true;
                }
            } catch (Exception e) {
                // 单条配置解析失败不影响其余条目，保守视为不匹配
            }
        }
        return false;
    }

    /** CIDR 匹配（按地址字节逐位比较前缀长度），仅处理同族地址 */
    private static boolean matchesCidr(String ip, String cidr) throws Exception {
        int slash = cidr.indexOf('/');
        InetAddress addr = InetAddress.getByName(ip);
        InetAddress net = InetAddress.getByName(cidr.substring(0, slash));
        int prefix = Integer.parseInt(cidr.substring(slash + 1).trim());
        byte[] a = addr.getAddress();
        byte[] n = net.getAddress();
        if (a.length != n.length) {
            return false;
        }
        int fullBytes = prefix / 8;
        int remBits = prefix % 8;
        for (int i = 0; i < fullBytes; i++) {
            if (a[i] != n[i]) {
                return false;
            }
        }
        if (remBits == 0) {
            return true;
        }
        int mask = 0xFF << (8 - remBits) & 0xFF;
        return (a[fullBytes] & mask) == (n[fullBytes] & mask);
    }
}
