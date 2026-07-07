package com.pzhu.mall.common.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RedisKeyPrefixTest {

    @Test
    void allPrefixes_nonEmptyAndUnique() {
        String[] prefixes = {
                RedisKeyPrefix.USER, RedisKeyPrefix.PRODUCT, RedisKeyPrefix.CART,
                RedisKeyPrefix.ORDER, RedisKeyPrefix.STOCK, RedisKeyPrefix.STOCK_LOCK,
                RedisKeyPrefix.BEHAVIOR, RedisKeyPrefix.RECOMMEND, RedisKeyPrefix.COUPON,
                RedisKeyPrefix.PROMOTION, RedisKeyPrefix.LOGISTICS, RedisKeyPrefix.STATISTICS,
                RedisKeyPrefix.DICT, RedisKeyPrefix.UPLOAD, RedisKeyPrefix.SEARCH_HISTORY,
        };

        for (String p : prefixes) {
            assertNotNull(p, "Prefix should not be null");
            assertFalse(p.isEmpty(), "Prefix should not be empty");
            assertTrue(p.contains(":"), "Prefix should follow 'mall:module' pattern, got: " + p);
        }

        // Check uniqueness
        assertEquals(prefixes.length, java.util.stream.Stream.of(prefixes).distinct().count(),
                "All prefixes must be unique");
    }

    @Test
    void stockAndLockPrefixes_different() {
        assertNotEquals(RedisKeyPrefix.STOCK, RedisKeyPrefix.STOCK_LOCK,
                "STOCK and STOCK_LOCK prefixes must differ");
    }

    @Test
    void allPrefixes_startWithMall() {
        java.lang.reflect.Field[] fields = RedisKeyPrefix.class.getDeclaredFields();
        for (java.lang.reflect.Field f : fields) {
            if (java.lang.reflect.Modifier.isStatic(f.getModifiers()) && f.getType() == String.class) {
                try {
                    String val = (String) f.get(null);
                    assertTrue(val.startsWith("mall:"),
                            f.getName() + " = " + val + " should start with 'mall:'");
                } catch (IllegalAccessException e) {
                    fail("Cannot access field: " + f.getName());
                }
            }
        }
    }
}
