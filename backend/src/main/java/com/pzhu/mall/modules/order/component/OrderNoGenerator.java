package com.pzhu.mall.modules.order.component;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 订单号生成器。
 *
 * <p>格式：yyyyMMddHHmmss + 6位随机数，保证同一毫秒内并发下单不重复。
 */
@Component
public class OrderNoGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public String generate() {
        String timestamp = FORMATTER.format(LocalDateTime.now());
        int random = ThreadLocalRandom.current().nextInt(1000000);
        return timestamp + String.format("%06d", random);
    }
}
