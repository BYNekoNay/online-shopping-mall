package com.pzhu.mall.modules.order.component;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * 订单号生成器。
 *
 * <p>格式：yyyyMMddHHmmss + 6位随机数，保证同一毫秒内并发下单不重复。
 */
@Component
public class OrderNoGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Random RANDOM = new Random();

    public String generate() {
        String timestamp = FORMATTER.format(LocalDateTime.now());
        String random = String.format("%06d", RANDOM.nextInt(1000000));
        return timestamp + random;
    }
}
