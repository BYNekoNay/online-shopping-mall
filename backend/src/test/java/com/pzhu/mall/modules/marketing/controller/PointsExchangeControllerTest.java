package com.pzhu.mall.modules.marketing.controller;

import com.pzhu.mall.common.result.PageResult;
import com.pzhu.mall.modules.marketing.entity.PointsGoods;
import com.pzhu.mall.modules.marketing.service.PointsExchangeService;
import com.pzhu.mall.security.LoginUserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PointsExchangeController 单元测试（E-1 覆盖率补测：marketing.controller 29% → ≥70%）。
 * <p>覆盖 PX-01~PX-04：商品列表/兑换/记录。</p>
 */
class PointsExchangeControllerTest {

    private PointsExchangeService pointsExchangeService;
    private PointsExchangeController controller;

    @BeforeEach
    void setUp() {
        pointsExchangeService = mock(PointsExchangeService.class);
        controller = new PointsExchangeController();
        inject(controller, "pointsExchangeService", pointsExchangeService);
    }

    @AfterEach
    void tearDown() {
        LoginUserContext.clear();
    }

    @Test
    void goods_returnsPage() {
        // PX-01：兑换商品列表
        var g = new PointsGoods();
        g.setId(1L);
        g.setName("帆布袋");
        when(pointsExchangeService.listGoods(1, 10))
                .thenReturn(new PageResult<>(1L, 1, 10, 1L, List.of(g)));

        var result = controller.goods(1, 10);

        assertEquals(1, result.getData().getRecords().size());
        verify(pointsExchangeService).listGoods(1, 10);
    }

    @Test
    void exchange_loggedIn_delegates() {
        // PX-02：兑换（登录）→ 委托 service，数量缺省 1
        LoginUserContext.set(100L, 1);
        PointsExchangeController.ExchangeDTO dto = new PointsExchangeController.ExchangeDTO();
        dto.setGoodsId(1L);

        var result = controller.exchange(dto);

        assertEquals(0, result.getCode());
        verify(pointsExchangeService).exchange(100L, 1L, 1);
    }

    @Test
    void exchange_withQuantity_usesProvided() {
        // PX-03：兑换指定数量
        LoginUserContext.set(100L, 1);
        PointsExchangeController.ExchangeDTO dto = new PointsExchangeController.ExchangeDTO();
        dto.setGoodsId(2L);
        dto.setQuantity(3);

        controller.exchange(dto);

        verify(pointsExchangeService).exchange(100L, 2L, 3);
    }

    @Test
    void myLogs_loggedIn_returnsRecords() {
        // PX-04：我的兑换记录
        LoginUserContext.set(100L, 1);
        var log = new com.pzhu.mall.modules.marketing.entity.PointsExchangeLog();
        log.setGoodsName("徽章");
        when(pointsExchangeService.listMyLogs(100L, 10)).thenReturn(List.of(log));

        var result = controller.myLogs(10);

        assertEquals(1, result.getData().size());
        verify(pointsExchangeService).listMyLogs(100L, 10);
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
