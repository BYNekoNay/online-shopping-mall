package com.pzhu.mall.modules.order.component;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 订单号生成器。
 *
 * <p>格式：时间戳（毫秒级，36进制）+ 序列号（4位36进制）<br>
 * 单实例下同一毫秒最多可生成 36^4 = 1,679,616 个不重复订单号。
 */
@Component
public class OrderNoGenerator {

    private static final AtomicInteger SEQUENCE = new AtomicInteger(0);
    private static final long MAX_SEQUENCE = 1679616; // 36^4
    private static final char[] DIGITS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    public String generate() {
        long millis = Instant.now().toEpochMilli();
        int seq = SEQUENCE.getAndIncrement();
        if (seq >= MAX_SEQUENCE) {
            SEQUENCE.set(0);
            seq = 0;
        }
        return toBase36(millis, 8) + toBase36(seq, 4);
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
