package com.pzhu.mall.modules.marketing.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.modules.marketing.entity.PointsExchangeLog;
import com.pzhu.mall.modules.marketing.entity.PointsGoods;
import com.pzhu.mall.modules.marketing.mapper.PointsExchangeLogMapper;
import com.pzhu.mall.modules.marketing.mapper.PointsGoodsMapper;
import com.pzhu.mall.modules.marketing.mapper.PointsRecordMapper;
import com.pzhu.mall.modules.user.entity.User;
import com.pzhu.mall.modules.user.mapper.UserMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PointsExchangeService 单元测试（C-1 积分商城兑换）。
 * <p>覆盖 E-01~E-05：积分充足/不足回滚/库存不足/并发/下架。</p>
 */
class PointsExchangeServiceTest {

    private PointsGoodsMapper pointsGoodsMapper;
    private PointsExchangeLogMapper pointsExchangeLogMapper;
    private PointsRecordMapper pointsRecordMapper;
    private UserMapper userMapper;
    private PointsExchangeService service;

    @BeforeAll
    static void initTableInfo() {
        var assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, PointsGoods.class);
        TableInfoHelper.initTableInfo(assistant, PointsExchangeLog.class);
        TableInfoHelper.initTableInfo(assistant, com.pzhu.mall.modules.marketing.entity.PointsRecord.class);
        TableInfoHelper.initTableInfo(assistant, User.class);
    }

    @BeforeEach
    void setUp() {
        pointsGoodsMapper = mock(PointsGoodsMapper.class);
        pointsExchangeLogMapper = mock(PointsExchangeLogMapper.class);
        pointsRecordMapper = mock(PointsRecordMapper.class);
        userMapper = mock(UserMapper.class);
        service = new PointsExchangeService();
        inject(service, "pointsGoodsMapper", pointsGoodsMapper);
        inject(service, "pointsExchangeLogMapper", pointsExchangeLogMapper);
        inject(service, "pointsRecordMapper", pointsRecordMapper);
        inject(service, "userMapper", userMapper);
    }

    private PointsGoods goods(Long id, int cost, int stock, int status) {
        PointsGoods g = new PointsGoods();
        g.setId(id);
        g.setName("积分商品" + id);
        g.setPointsCost(cost);
        g.setStock(stock);
        g.setStatus(status);
        return g;
    }

    @Test
    void exchange_sufficientPoints_succeeds() {
        // E-01：积分充足 → 成功，库存/积分双扣，流水+快照生成
        when(pointsGoodsMapper.selectById(1L)).thenReturn(goods(1L, 100, 10, 1));
        when(pointsGoodsMapper.update(isNull(), any())).thenReturn(1); // 扣库存成功
        when(userMapper.update(isNull(), any())).thenReturn(1);        // 扣积分成功

        service.exchange(100L, 1L, 2);

        verify(pointsGoodsMapper).update(isNull(), any());   // 库存扣 2
        verify(userMapper).update(isNull(), any());          // 积分扣 200
        verify(pointsRecordMapper).insert(any());            // type=5 流水
        verify(pointsExchangeLogMapper).insert(any());       // 兑换记录
    }

    @Test
    void exchange_insufficientPoints_rollsBackStock() {
        // E-02：积分不足 → 抛错且库存回滚（+quantity）
        when(pointsGoodsMapper.selectById(1L)).thenReturn(goods(1L, 100, 10, 1));
        when(pointsGoodsMapper.update(isNull(), any())).thenReturn(1); // 扣库存成功
        when(userMapper.update(isNull(), any())).thenReturn(0);        // 扣积分失败

        BusinessException ex = assertThrows(BusinessException.class, () -> service.exchange(100L, 1L, 1));
        assertTrue(ex.getMessage().contains("积分不足"));
        // 回滚库存：第二次 update 调用（参数同扣减）
        verify(pointsGoodsMapper, times(2)).update(isNull(), any());
        verify(pointsRecordMapper, never()).insert(any());
    }

    @Test
    void exchange_insufficientStock_throws() {
        // E-03：库存不足 → 抛错且积分不变（未走到扣积分）
        when(pointsGoodsMapper.selectById(1L)).thenReturn(goods(1L, 100, 1, 1));
        when(pointsGoodsMapper.update(isNull(), any())).thenReturn(0); // 扣库存失败（原子条件不满足）

        BusinessException ex = assertThrows(BusinessException.class, () -> service.exchange(100L, 1L, 5));
        assertTrue(ex.getMessage().contains("库存不足"));
        verify(userMapper, never()).update(isNull(), any());
    }

    @Test
    void exchange_concurrent_atomicGuarded() {
        // E-04：并发场景原子 UPDATE（WHERE stock>=qty）——mock 返回 0 模拟被并发抢完
        when(pointsGoodsMapper.selectById(1L)).thenReturn(goods(1L, 100, 3, 1));
        when(pointsGoodsMapper.update(isNull(), any())).thenReturn(0);

        assertThrows(BusinessException.class, () -> service.exchange(100L, 1L, 3));
        verify(userMapper, never()).update(isNull(), any());
    }

    @Test
    void exchange_offlineGoods_rejected() {
        // E-05：下架商品 → 拒绝
        when(pointsGoodsMapper.selectById(1L)).thenReturn(goods(1L, 100, 10, 0));
        assertThrows(BusinessException.class, () -> service.exchange(100L, 1L, 1));
        verify(pointsGoodsMapper, never()).update(isNull(), any());
    }

    @Test
    void exchange_quantityNonPositive_throws() {
        // E-00：数量≤0 → 拒绝
        when(pointsGoodsMapper.selectById(1L)).thenReturn(goods(1L, 100, 10, 1));
        assertThrows(BusinessException.class, () -> service.exchange(100L, 1L, 0));
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
