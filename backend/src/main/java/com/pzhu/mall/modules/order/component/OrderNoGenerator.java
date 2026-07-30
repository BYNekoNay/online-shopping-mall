package com.pzhu.mall.modules.order.component;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 订单号生成器。
 *
 * <p>格式：时间戳（毫秒级，8位36进制）+ 实例标识（4位36进制）+ 序列号（4位36进制）。<br>
 * H-10 修复：引入 JVM 启动时随机生成的实例标识，多实例部署时不同实例的订单号不会重复；
 * 序列号改为取模复用，消除原先"达到上限后重置"的竞态。order_no 列为 VARCHAR(32)，当前长度 16 位。
 */
@Component
public class OrderNoGenerator {

    private static final AtomicInteger SEQUENCE = new AtomicInteger(0);
    private static final int MAX_SEQUENCE = 1679616; // 36^4
    private static final char[] DIGITS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    /** 实例标识：JVM 启动时随机生成，用于区分不同实例生成的订单号 */
    private static final String INSTANCE_ID = generateInstanceId();

    public String generate() {
        long millis = Instant.now().toEpochMilli();
        // floorMod 处理计数器溢出为负的情况，保证序列号始终为 [0, 36^4)
        int seq = Math.floorMod(SEQUENCE.getAndIncrement(), MAX_SEQUENCE);
        return toBase36(millis, 8) + INSTANCE_ID + toBase36(seq, 4);
    }

    private static String generateInstanceId() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(4);
        for (int i = 0; i < 4; i++) {
            sb.append(DIGITS[random.nextInt(36)]);
        }
        return sb.toString();
    }

    private static String toBase36(long value, int minLen) {
        StringBuilder sb = new StringBuilder();
        while (value > 0) {
            sb.append(DIGITS[(int) (value % 36)]);
            value /= 36;
        }
        while (sb.length() < minLen) {
            sb.append('0');
        }
        return sb.reverse().toString();
    }
}
