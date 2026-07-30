package com.pzhu.mall.modules.order.task;

import com.pzhu.mall.modules.order.entity.Order;
import com.pzhu.mall.modules.order.mapper.OrderMapper;
import com.pzhu.mall.modules.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 6.12 OrderTimeoutTask 单元测试。
 * <p>H-9 修复验证：超时阈值仅在 Java 侧计算一次（now - timeoutMinutes）后传入 SQL，
 * 不再与 SQL 的 DATE_SUB 叠减导致超时时间减半。</p>
 */
class OrderTimeoutTaskTest {

    private OrderMapper orderMapper;
    private OrderService orderService;
    private RedissonClient redissonClient;
    private StringRedisTemplate stringRedisTemplate;
    private ValueOperations<String, String> valueOperations;
    private OrderTimeoutTask task;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        orderMapper = mock(OrderMapper.class);
        orderService = mock(OrderService.class);
        redissonClient = mock(RedissonClient.class);
        stringRedisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        task = new OrderTimeoutTask();
        inject(task, "orderMapper", orderMapper);
        inject(task, "orderService", orderService);
        inject(task, "redissonClient", redissonClient);
        inject(task, "stringRedisTemplate", stringRedisTemplate);
        inject(task, "timeoutMinutes", 30);
    }

    @Test
    void cancelTimeoutOrders_thresholdSubtractedOnce() {
        // H-9 修复验证：传入 SQL 的阈值 = now - 30min（单次减法，允许秒级执行误差）
        when(orderMapper.selectTimeoutUnpaidOrders(any())).thenReturn(Collections.emptyList());

        task.cancelTimeoutOrders();

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(orderMapper).selectTimeoutUnpaidOrders(captor.capture());
        LocalDateTime expected = LocalDateTime.now().minusMinutes(30);
        long driftSeconds = Math.abs(Duration.between(captor.getValue(), expected).getSeconds());
        assertTrue(driftSeconds < 5, "阈值应为 now-30min，实际偏差 " + driftSeconds + " 秒");
    }

    @Test
    void cancelTimeoutOrders_cancelsEachOrderViaSystemEntry() {
        Order o1 = new Order();
        o1.setId(1L);
        Order o2 = new Order();
        o2.setId(2L);
        when(orderMapper.selectTimeoutUnpaidOrders(any())).thenReturn(Arrays.asList(o1, o2));
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);

        task.cancelTimeoutOrders();

        verify(orderService).cancelOrderBySystem(1L);
        verify(orderService).cancelOrderBySystem(2L);
    }

    @Test
    void cancelTimeoutOrders_lockTakenByOtherInstance_skips() {
        Order o1 = new Order();
        o1.setId(1L);
        when(orderMapper.selectTimeoutUnpaidOrders(any())).thenReturn(Collections.singletonList(o1));
        // 其他实例已持有取消锁
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(false);

        task.cancelTimeoutOrders();

        verify(orderService, never()).cancelOrderBySystem(anyLong());
    }

    private static void inject(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
