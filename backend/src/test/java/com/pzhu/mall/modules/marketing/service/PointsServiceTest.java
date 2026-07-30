package com.pzhu.mall.modules.marketing.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.modules.marketing.entity.PointsRecord;
import com.pzhu.mall.modules.marketing.mapper.PointsRecordMapper;
import com.pzhu.mall.modules.user.entity.User;
import com.pzhu.mall.modules.user.mapper.UserMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * 6.4 PointsService 单元测试。
 * <p>覆盖抵扣额度计算、原子扣减（并发防负数）、支付发放（HALF_UP）、
 * 退款扣回（H-11：扣减失败不记流水）。</p>
 */
class PointsServiceTest {

    private UserMapper userMapper;
    private PointsRecordMapper pointsRecordMapper;
    private PointsService service;

    @BeforeAll
    static void initTableInfo() {
        var assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, User.class);
        TableInfoHelper.initTableInfo(assistant, PointsRecord.class);
    }

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        pointsRecordMapper = mock(PointsRecordMapper.class);
        service = new PointsService();
        inject(service, "userMapper", userMapper);
        inject(service, "pointsRecordMapper", pointsRecordMapper);
    }

    // ==================== calculateDeduct ====================

    @Test
    void calculateDeduct_limitedByPoints() {
        // 500 积分 → 5.00 元；商品 20 元 → 上限 10 元；取小 = 5.00
        when(userMapper.selectById(1L)).thenReturn(user(500));
        BigDecimal[] r = service.calculateDeduct(1L, new BigDecimal("20"));
        assertEquals(new BigDecimal("5.00"), r[0]);
        assertEquals(new BigDecimal(500), r[1]);
    }

    @Test
    void calculateDeduct_limitedByGoodsAmount() {
        // 100000 积分 → 1000 元；商品 20 元 → 上限 10 元
        when(userMapper.selectById(1L)).thenReturn(user(100000));
        BigDecimal[] r = service.calculateDeduct(1L, new BigDecimal("20"));
        assertEquals(new BigDecimal("10.0"), r[0]);
        assertEquals(new BigDecimal(1000), r[1]);
    }

    @Test
    void calculateDeduct_userMissing_returnsZero() {
        when(userMapper.selectById(1L)).thenReturn(null);
        BigDecimal[] r = service.calculateDeduct(1L, new BigDecimal("20"));
        assertEquals(BigDecimal.ZERO, r[0]);
        assertEquals(BigDecimal.ZERO, r[1]);
    }

    // ==================== settleDeduct ====================

    @Test
    void settleDeduct_success_writesNegativeRecord() {
        when(userMapper.update(isNull(), any())).thenReturn(1);

        service.settleDeduct(1L, 300, 55L);

        ArgumentCaptor<PointsRecord> captor = ArgumentCaptor.forClass(PointsRecord.class);
        verify(pointsRecordMapper).insert(captor.capture());
        assertEquals(-300, captor.getValue().getChangeAmount());
        assertEquals(2, captor.getValue().getType());
        assertEquals(55L, captor.getValue().getRelatedOrderId());
    }

    @Test
    void settleDeduct_insufficientPoints_throws() {
        // 原子 UPDATE（points >= ?）影响 0 行且用户存在 → 积分不足
        when(userMapper.update(isNull(), any())).thenReturn(0);
        when(userMapper.selectById(1L)).thenReturn(user(10));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.settleDeduct(1L, 300, 55L));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
        verify(pointsRecordMapper, never()).insert(any(PointsRecord.class));
    }

    @Test
    void settleDeduct_userMissing_silentSkip() {
        when(userMapper.update(isNull(), any())).thenReturn(0);
        when(userMapper.selectById(1L)).thenReturn(null);

        assertDoesNotThrow(() -> service.settleDeduct(1L, 300, 55L));
        verify(pointsRecordMapper, never()).insert(any(PointsRecord.class));
    }

    @Test
    void settleDeduct_zeroPoints_noop() {
        service.settleDeduct(1L, 0, 55L);
        verify(userMapper, never()).update(any(), any());
        verify(pointsRecordMapper, never()).insert(any(PointsRecord.class));
    }

    // ==================== settleEarn ====================

    @Test
    void settleEarn_halfUpRounding() {
        // M9 修复验证：199.5 → HALF_UP → 200（旧 ROUND_DOWN 为 199）
        service.settleEarn(55L, 1L, new BigDecimal("199.50"));

        verify(userMapper).update(isNull(), any());
        ArgumentCaptor<PointsRecord> captor = ArgumentCaptor.forClass(PointsRecord.class);
        verify(pointsRecordMapper).insert(captor.capture());
        assertEquals(200, captor.getValue().getChangeAmount());
        assertEquals(1, captor.getValue().getType());
    }

    @Test
    void settleEarn_zeroPoints_skips() {
        service.settleEarn(55L, 1L, new BigDecimal("0.40"));
        verify(userMapper, never()).update(any(), any());
        verify(pointsRecordMapper, never()).insert(any(PointsRecord.class));
    }

    // ==================== clawback (H-11) ====================

    @Test
    void clawback_success_writesNegativeRecord() {
        PointsRecord earn = new PointsRecord();
        earn.setUserId(1L);
        earn.setChangeAmount(100);
        earn.setType(1);
        when(pointsRecordMapper.selectList(any())).thenReturn(Collections.singletonList(earn));
        when(userMapper.update(isNull(), any())).thenReturn(1);

        service.clawback(55L);

        ArgumentCaptor<PointsRecord> captor = ArgumentCaptor.forClass(PointsRecord.class);
        verify(pointsRecordMapper).insert(captor.capture());
        assertEquals(-100, captor.getValue().getChangeAmount());
        assertEquals(3, captor.getValue().getType());
        assertEquals(1L, captor.getValue().getUserId());
    }

    @Test
    void clawback_insufficientPoints_skipsRecord() {
        // H-11 修复验证：原子扣减失败（积分不足）时不插入扣减流水，避免账实不符
        PointsRecord earn = new PointsRecord();
        earn.setUserId(1L);
        earn.setChangeAmount(100);
        when(pointsRecordMapper.selectList(any())).thenReturn(Collections.singletonList(earn));
        when(userMapper.update(isNull(), any())).thenReturn(0);

        assertDoesNotThrow(() -> service.clawback(55L));
        verify(pointsRecordMapper, never()).insert(any(PointsRecord.class));
    }

    @Test
    void clawback_noEarnRecord_noop() {
        when(pointsRecordMapper.selectList(any())).thenReturn(Collections.emptyList());
        assertDoesNotThrow(() -> service.clawback(55L));
        verify(userMapper, never()).update(any(), any());
    }

    // ==================== refundDeduct（H-5） ====================

    @Test
    void refundDeduct_success_returnsDeductedPoints() {
        // H-5 修复验证：type=2 抵扣流水（-500）→ 原子加回 500 + 写 type=4 返还流水
        when(pointsRecordMapper.selectCount(any())).thenReturn(0L);
        PointsRecord deduct = new PointsRecord();
        deduct.setUserId(1L);
        deduct.setChangeAmount(-500);
        deduct.setType(2);
        deduct.setRelatedOrderId(55L);
        when(pointsRecordMapper.selectList(any())).thenReturn(Collections.singletonList(deduct));
        when(userMapper.update(isNull(), any())).thenReturn(1);

        service.refundDeduct(55L);

        verify(userMapper).update(isNull(), any()); // points = points + 500
        ArgumentCaptor<PointsRecord> captor = ArgumentCaptor.forClass(PointsRecord.class);
        verify(pointsRecordMapper).insert(captor.capture());
        assertEquals(500, captor.getValue().getChangeAmount());
        assertEquals(4, captor.getValue().getType());
        assertEquals(1L, captor.getValue().getUserId());
    }

    @Test
    void refundDeduct_noDeductRecord_noop() {
        when(pointsRecordMapper.selectCount(any())).thenReturn(0L);
        when(pointsRecordMapper.selectList(any())).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> service.refundDeduct(55L));
        verify(userMapper, never()).update(any(), any());
        verify(pointsRecordMapper, never()).insert(any(PointsRecord.class));
    }

    @Test
    void refundDeduct_alreadyReturned_idempotentSkip() {
        // 幂等验证：同订单已存在 type=4 返还记录时直接跳过，防止重复返还
        when(pointsRecordMapper.selectCount(any())).thenReturn(1L);

        service.refundDeduct(55L);

        verify(pointsRecordMapper, never()).selectList(any());
        verify(userMapper, never()).update(any(), any());
        verify(pointsRecordMapper, never()).insert(any(PointsRecord.class));
    }

    // ==================== helpers ====================

    private static User user(int points) {
        User u = new User();
        u.setId(1L);
        u.setPoints(points);
        return u;
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
