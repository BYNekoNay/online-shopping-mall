package com.pzhu.mall.modules.order.controller;

import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.modules.logistics.service.FreightService;
import com.pzhu.mall.modules.marketing.service.CouponService;
import com.pzhu.mall.modules.marketing.service.PointsService;
import com.pzhu.mall.modules.marketing.service.PromotionService;
import com.pzhu.mall.modules.order.dto.ProductItemDTO;
import com.pzhu.mall.modules.order.entity.OrderItem;
import com.pzhu.mall.modules.order.mapper.OrderItemMapper;
import com.pzhu.mall.modules.order.vo.OrderEstimateVO;
import com.pzhu.mall.modules.product.entity.Product;
import com.pzhu.mall.modules.product.mapper.ProductMapper;
import com.pzhu.mall.modules.product.mapper.SkuMapper;
import com.pzhu.mall.modules.shop.mapper.ShopMapper;
import com.pzhu.mall.modules.user.entity.Address;
import com.pzhu.mall.modules.user.mapper.AddressMapper;
import com.pzhu.mall.security.LoginUserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OrderEstimateController 单元测试（下单估价：按店铺分组/运费/优惠）。
 */
class OrderEstimateControllerTest {

    private OrderItemMapper orderItemMapper;
    private ProductMapper productMapper;
    private SkuMapper skuMapper;
    private AddressMapper addressMapper;
    private FreightService freightService;
    private PromotionService promotionService;
    private CouponService couponService;
    private PointsService pointsService;
    private ShopMapper shopMapper;
    private OrderEstimateController controller;

    @BeforeEach
    void setUp() {
        orderItemMapper = mock(OrderItemMapper.class);
        productMapper = mock(ProductMapper.class);
        skuMapper = mock(SkuMapper.class);
        addressMapper = mock(AddressMapper.class);
        freightService = mock(FreightService.class);
        promotionService = mock(PromotionService.class);
        couponService = mock(CouponService.class);
        pointsService = mock(PointsService.class);
        shopMapper = mock(ShopMapper.class);
        controller = new OrderEstimateController();
        inject(controller, "orderItemMapper", orderItemMapper);
        inject(controller, "productMapper", productMapper);
        inject(controller, "skuMapper", skuMapper);
        inject(controller, "addressMapper", addressMapper);
        inject(controller, "freightService", freightService);
        inject(controller, "promotionService", promotionService);
        inject(controller, "couponService", couponService);
        inject(controller, "pointsService", pointsService);
        inject(controller, "shopMapper", shopMapper);
        LoginUserContext.set(100L, 1);
        when(promotionService.matchActive(any())).thenReturn(Collections.emptyList());
        when(promotionService.calculateDiscount(anyLong(), any())).thenReturn(BigDecimal.ZERO);
        when(pointsService.calculateDeduct(anyLong(), any())).thenReturn(new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        when(shopMapper.selectById(anyLong())).thenReturn(new com.pzhu.mall.modules.shop.entity.Shop());
    }

    @AfterEach
    void tearDown() {
        LoginUserContext.clear();
    }

    @Test
    void estimate_emptyItems_returnsEmpty() {
        OrderEstimateController.EstimateRequest req = new OrderEstimateController.EstimateRequest();
        req.setAddressId(1L);
        req.setProductItems(Collections.emptyList());

        Result<List<OrderEstimateVO>> result = controller.estimate(req);

        assertTrue(result.getData().isEmpty());
    }

    @Test
    void estimate_groupsByShopAndCalculates() {
        // 两个商品同属店铺 1 → 合并为一组
        Address addr = new Address();
        addr.setId(1L);
        addr.setProvince("广东");
        when(addressMapper.selectById(1L)).thenReturn(addr);

        Product p1 = product(10L, 1L, new BigDecimal("100"), 1);
        Product p2 = product(11L, 1L, new BigDecimal("200"), 1);
        when(productMapper.selectById(10L)).thenReturn(p1);
        when(productMapper.selectById(11L)).thenReturn(p2);
        when(freightService.calculate(eq(1L), anyString(), any())).thenReturn(new BigDecimal("10"));

        OrderEstimateController.EstimateRequest req = new OrderEstimateController.EstimateRequest();
        req.setAddressId(1L);
        req.setProductItems(List.of(item(10L, 1), item(11L, 2)));

        Result<List<OrderEstimateVO>> result = controller.estimate(req);

        assertEquals(1, result.getData().size());
        assertEquals(1L, result.getData().get(0).getShopId());
    }

    @Test
    void estimate_offlineProduct_skipped() {
        Address addr = new Address();
        addr.setId(1L);
        addr.setProvince("广东");
        when(addressMapper.selectById(1L)).thenReturn(addr);

        // status=2 下架 → 跳过
        Product offline = product(10L, 1L, new BigDecimal("100"), 2);
        when(productMapper.selectById(10L)).thenReturn(offline);

        OrderEstimateController.EstimateRequest req = new OrderEstimateController.EstimateRequest();
        req.setAddressId(1L);
        req.setProductItems(List.of(item(10L, 1)));

        Result<List<OrderEstimateVO>> result = controller.estimate(req);

        assertTrue(result.getData().isEmpty());
    }

    private static ProductItemDTO item(Long productId, int quantity) {
        ProductItemDTO dto = new ProductItemDTO();
        dto.setProductId(productId);
        dto.setQuantity(quantity);
        return dto;
    }

    private static Product product(Long id, Long shopId, BigDecimal price, int status) {
        Product p = new Product();
        p.setId(id);
        p.setShopId(shopId);
        p.setPrice(price);
        p.setStatus(status);
        p.setIsDeleted(0);
        return p;
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
