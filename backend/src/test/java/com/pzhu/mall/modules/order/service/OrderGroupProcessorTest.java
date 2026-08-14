package com.pzhu.mall.modules.order.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.pzhu.mall.modules.behavior.service.BehaviorService;
import com.pzhu.mall.modules.cart.entity.Cart;
import com.pzhu.mall.modules.cart.mapper.CartMapper;
import com.pzhu.mall.modules.logistics.service.FreightService;
import com.pzhu.mall.modules.marketing.service.CouponService;
import com.pzhu.mall.modules.marketing.service.PointsService;
import com.pzhu.mall.modules.marketing.service.PromotionService;
import com.pzhu.mall.modules.order.component.OrderNoGenerator;
import com.pzhu.mall.modules.order.component.StockService;
import com.pzhu.mall.modules.order.dto.CreateOrderDTO;
import com.pzhu.mall.modules.order.dto.ProductItemDTO;
import com.pzhu.mall.modules.order.entity.Order;
import com.pzhu.mall.modules.order.entity.OrderItem;
import com.pzhu.mall.modules.order.mapper.OrderItemMapper;
import com.pzhu.mall.modules.order.mapper.OrderMapper;
import com.pzhu.mall.modules.product.entity.Product;
import com.pzhu.mall.modules.product.mapper.ProductMapper;
import com.pzhu.mall.modules.product.mapper.SkuMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 6.11 OrderGroupProcessor 单元测试。
 * <p>H-6 修复验证：优惠券为单一资源，仅在 applyCoupon=true 的分组内计价并核销一次；
 * applyCoupon=false 的分组不得触碰优惠券的任何状态。</p>
 */
class OrderGroupProcessorTest {

    private OrderMapper orderMapper;
    private OrderItemMapper orderItemMapper;
    private OrderNoGenerator orderNoGenerator;
    private ProductMapper productMapper;
    private SkuMapper skuMapper;
    private FreightService freightService;
    private PromotionService promotionService;
    private PointsService pointsService;
    private CouponService couponService;
    private BehaviorService behaviorService;
    private CartMapper cartMapper;
    private StockService stockService;
    private OrderGroupProcessor processor;

    @BeforeAll
    static void initTableInfo() {
        var assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Order.class);
        TableInfoHelper.initTableInfo(assistant, OrderItem.class);
        // H-3 修复：购物车条件删除使用 Cart 的 LambdaQueryWrapper，需初始化其 TableInfo
        TableInfoHelper.initTableInfo(assistant, Cart.class);
    }

    @BeforeEach
    void setUp() {
        orderMapper = mock(OrderMapper.class);
        orderItemMapper = mock(OrderItemMapper.class);
        orderNoGenerator = mock(OrderNoGenerator.class);
        productMapper = mock(ProductMapper.class);
        skuMapper = mock(SkuMapper.class);
        freightService = mock(FreightService.class);
        promotionService = mock(PromotionService.class);
        pointsService = mock(PointsService.class);
        couponService = mock(CouponService.class);
        behaviorService = mock(BehaviorService.class);
        cartMapper = mock(CartMapper.class);
        stockService = mock(StockService.class);

        processor = new OrderGroupProcessor();
        inject(processor, "orderMapper", orderMapper);
        inject(processor, "orderItemMapper", orderItemMapper);
        inject(processor, "orderNoGenerator", orderNoGenerator);
        inject(processor, "productMapper", productMapper);
        inject(processor, "skuMapper", skuMapper);
        inject(processor, "freightService", freightService);
        inject(processor, "promotionService", promotionService);
        inject(processor, "pointsService", pointsService);
        inject(processor, "couponService", couponService);
        inject(processor, "behaviorService", behaviorService);
        inject(processor, "cartMapper", cartMapper);
        inject(processor, "stockService", stockService);

        // 公共桩：单商品 100 元、免运费、无促销、订单号生成
        Product product = new Product();
        product.setId(10L);
        product.setName("商品A");
        product.setPrice(new BigDecimal("100"));
        when(productMapper.selectById(10L)).thenReturn(product);
        when(freightService.calculate(eq(1L), eq("GD"), any())).thenReturn(BigDecimal.ZERO);
        when(promotionService.calculateDiscount(eq(1L), any())).thenReturn(BigDecimal.ZERO);
        when(orderNoGenerator.generate()).thenReturn("TESTNO");
        when(orderMapper.insert(any(Order.class))).thenReturn(1);
        when(orderItemMapper.insert(any(OrderItem.class))).thenReturn(1);
        when(orderItemMapper.selectList(any())).thenReturn(Collections.emptyList());
    }

    private CreateOrderDTO dtoWithCoupon(Long userCouponId) {
        CreateOrderDTO dto = new CreateOrderDTO();
        dto.setUserCouponId(userCouponId);
        return dto;
    }

    private ProductItemDTO item(Long productId, int quantity) {
        ProductItemDTO pi = new ProductItemDTO();
        pi.setProductId(productId);
        pi.setQuantity(quantity);
        return pi;
    }

    @Test
    void processGroup_applyCouponTrue_calculatesDiscountAndMarksUsed() {
        // M-02 修复：改用带适用范围校验的重载（userCouponId, goodsAmount, userId, shopId, categoryIds）
        when(couponService.calculateDiscountByUserCoupon(eq(7L), any(), eq(100L), eq(1L), any()))
                .thenReturn(new BigDecimal("10.00"));

        processor.processGroup(100L, 1L, Collections.singletonList(item(10L, 1)),
                "{}", "GD", dtoWithCoupon(7L), false, true, null);

        // 计价并核销各一次
        verify(couponService).calculateDiscountByUserCoupon(eq(7L), any(), eq(100L), eq(1L), any());
        verify(couponService).markUsed(eq(7L), any(), eq(100L));

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderMapper).insert(captor.capture());
        Order saved = captor.getValue();
        assertEquals(new BigDecimal("10.00"), saved.getCouponDiscountAmount());
        // 实付 = 100 - 10 = 90
        assertEquals(0, new BigDecimal("90").compareTo(saved.getPayAmount()));
    }

    @Test
    void processGroup_applyCouponFalse_neverTouchesCoupon() {
        // H-6 修复验证：后续分组（applyCoupon=false）不得再计价、不得再核销，
        // 否则第二分组的 markUsed 会因券已被首组核销（WHERE status=0 命中 0 行）而抛异常
        processor.processGroup(100L, 1L, Collections.singletonList(item(10L, 1)),
                "{}", "GD", dtoWithCoupon(7L), false, false, null);

        verify(couponService, never()).calculateDiscountByUserCoupon(anyLong(), any(), any(), any(), any());
        verify(couponService, never()).markUsed(anyLong(), any(), anyLong());

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderMapper).insert(captor.capture());
        Order saved = captor.getValue();
        // 该分组不享受券抵扣，实付 = 商品金额
        assertEquals(0, BigDecimal.ZERO.compareTo(saved.getCouponDiscountAmount()));
        assertEquals(0, new BigDecimal("100").compareTo(saved.getPayAmount()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void processGroup_deleteCartItems_scopedByUserId() {
        // H-3 修复验证：购物车清理必须附加 user_id 条件（条件删除），
        // 不再使用无归属过滤的 deleteBatchIds，防止越权删除他人购物车项
        processor.processGroup(100L, 1L, Collections.singletonList(item(10L, 1)),
                "{}", "GD", dtoWithCoupon(null), false, false, Collections.singletonList(55L));

        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Cart>> captor =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class);
        verify(cartMapper).delete(captor.capture());
        verify(cartMapper, never()).deleteBatchIds(any());

        var wrapper = captor.getValue();
        // 先取 SQL 片段触发参数物化（MP 的 paramNameValuePairs 为惰性填充）
        String sql = wrapper.getSqlSegment();
        // 删除条件中包含 user_id 列，确保只删自己的购物车项
        assertTrue(sql.contains("user_id"), "SQL 条件应包含 user_id 列");
        assertTrue(wrapper.getParamNameValuePairs().containsValue(100L),
                "购物车删除必须按当前用户 ID 过滤");
    }

    @Test
    void processGroup_noCartItems_skipsCartDeletion() {
        // M-8 修复验证：本分组无来源购物车项（直接下单）时不触发任何购物车删除
        processor.processGroup(100L, 1L, Collections.singletonList(item(10L, 1)),
                "{}", "GD", dtoWithCoupon(null), false, false, null);

        verify(cartMapper, never()).delete(any());
        verify(cartMapper, never()).deleteBatchIds(any());
    }

    // ==================== M-01 满赠促销用例 ====================

    @Test
    void processGroup_giftThresholdMet_addsGiftItemAndDeductsStock() {
        // 满赠达标：金额 200 ≥ threshold 150 → 插入赠品行（isGift=1, price=0）+ 预扣赠品库存
        Product product = new Product();
        product.setId(10L);
        product.setName("商品A");
        product.setPrice(new BigDecimal("100"));
        when(productMapper.selectById(10L)).thenReturn(product);

        Product giftProduct = new Product();
        giftProduct.setId(5001L);
        giftProduct.setName("赠品B");
        giftProduct.setIsDeleted(0);
        when(productMapper.selectById(5001L)).thenReturn(giftProduct);

        com.pzhu.mall.modules.product.entity.Sku giftSku = new com.pzhu.mall.modules.product.entity.Sku();
        giftSku.setId(50011L);
        giftSku.setProductId(5001L);
        giftSku.setImage("gift.jpg");
        when(skuMapper.selectById(50011L)).thenReturn(giftSku);

        when(promotionService.matchGift(eq(1L), any()))
                .thenReturn(new PromotionService.GiftInfo(5001L, 50011L, 2));
        when(stockService.deduct(50011L, 2)).thenReturn(true);

        processor.processGroup(100L, 1L, java.util.Arrays.asList(item(10L, 2)),
                "{}", "GD", dtoWithCoupon(null), false, false, null);

        // 赠品库存被预扣
        verify(stockService).deduct(50011L, 2);
        // 订单明细中应有赠品行（isGift=1, price=0, qty=2）
        ArgumentCaptor<OrderItem> captor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemMapper, atLeast(2)).insert(captor.capture());
        OrderItem giftItem = captor.getAllValues().stream()
                .filter(oi -> oi.getIsGift() != null && oi.getIsGift() == 1)
                .findFirst().orElse(null);
        assertNotNull(giftItem, "应存在赠品行");
        assertEquals(5001L, giftItem.getProductId());
        assertEquals(50011L, giftItem.getSkuId());
        assertEquals(0, giftItem.getPrice().compareTo(BigDecimal.ZERO));
        assertEquals(2, giftItem.getQuantity());
        // 赠品不参与购买行为埋点（赠品 productId 不应被记录）
        verify(behaviorService, never()).record(eq(100L), eq(5001L), eq(3));
    }

    @Test
    void processGroup_giftNotMet_skipsGift() {
        // 满赠不达标：金额 100 < threshold 150 → 无赠品行、不扣赠品库存
        when(promotionService.matchGift(eq(1L), any())).thenReturn(null);

        processor.processGroup(100L, 1L, Collections.singletonList(item(10L, 1)),
                "{}", "GD", dtoWithCoupon(null), false, false, null);

        verify(stockService, never()).deduct(anyLong(), anyInt());
        ArgumentCaptor<OrderItem> captor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemMapper, atMost(1)).insert(captor.capture());
        assertTrue(captor.getAllValues().stream()
                .noneMatch(oi -> oi.getIsGift() != null && oi.getIsGift() == 1));
    }

    @Test
    void processGroup_giftStockNotEnough_skipsGiftWithoutBlocking() {
        // 赠品库存不足：deduct 返回 false → 静默跳过赠送，订单正常创建
        Product giftProduct = new Product();
        giftProduct.setId(5001L);
        giftProduct.setName("赠品B");
        giftProduct.setIsDeleted(0);
        when(productMapper.selectById(5001L)).thenReturn(giftProduct);
        com.pzhu.mall.modules.product.entity.Sku giftSku = new com.pzhu.mall.modules.product.entity.Sku();
        giftSku.setId(50011L);
        giftSku.setProductId(5001L);
        when(skuMapper.selectById(50011L)).thenReturn(giftSku);

        when(promotionService.matchGift(eq(1L), any()))
                .thenReturn(new PromotionService.GiftInfo(5001L, 50011L, 1));
        when(stockService.deduct(50011L, 1)).thenReturn(false);

        processor.processGroup(100L, 1L, Collections.singletonList(item(10L, 1)),
                "{}", "GD", dtoWithCoupon(null), false, false, null);

        verify(stockService).deduct(50011L, 1);
        verify(orderMapper).insert(any(Order.class)); // 订单未被阻断
    }

    @Test
    void processGroup_giftSkuBindingInvalid_skipsGift() {
        // 赠品 SKU 不属于赠品商品：跳过赠送
        Product giftProduct = new Product();
        giftProduct.setId(5001L);
        giftProduct.setName("赠品B");
        giftProduct.setIsDeleted(0);
        when(productMapper.selectById(5001L)).thenReturn(giftProduct);
        com.pzhu.mall.modules.product.entity.Sku wrongSku = new com.pzhu.mall.modules.product.entity.Sku();
        wrongSku.setId(50011L);
        wrongSku.setProductId(9999L); // 绑定错误
        when(skuMapper.selectById(50011L)).thenReturn(wrongSku);

        when(promotionService.matchGift(eq(1L), any()))
                .thenReturn(new PromotionService.GiftInfo(5001L, 50011L, 1));

        processor.processGroup(100L, 1L, Collections.singletonList(item(10L, 1)),
                "{}", "GD", dtoWithCoupon(null), false, false, null);

        verify(stockService, never()).deduct(anyLong(), anyInt());
        verify(orderMapper).insert(any(Order.class)); // 订单未被阻断
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
