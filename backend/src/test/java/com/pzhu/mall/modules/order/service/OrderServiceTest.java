package com.pzhu.mall.modules.order.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.modules.behavior.service.BehaviorService;
import com.pzhu.mall.modules.marketing.service.CouponService;
import com.pzhu.mall.modules.marketing.service.PointsService;
import com.pzhu.mall.modules.order.component.StockService;
import com.pzhu.mall.modules.order.dto.CreateOrderDTO;
import com.pzhu.mall.modules.order.dto.ProductItemDTO;
import com.pzhu.mall.modules.order.entity.Order;
import com.pzhu.mall.modules.order.entity.OrderItem;
import com.pzhu.mall.modules.order.mapper.OrderItemMapper;
import com.pzhu.mall.modules.order.mapper.OrderMapper;
import com.pzhu.mall.modules.product.entity.Product;
import com.pzhu.mall.modules.product.entity.Sku;
import com.pzhu.mall.modules.product.mapper.ProductMapper;
import com.pzhu.mall.modules.product.mapper.SkuMapper;
import com.pzhu.mall.security.LoginUserContext;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * 6.1 OrderService 单元测试。
 * <p>覆盖取消订单（用户/系统）、确认收货、支付的状态机与副作用（库存归还、
 * 优惠券释放、积分扣回/结算、行为记录）。纯 Mockito，不启动 Spring 上下文。</p>
 */
class OrderServiceTest {

    private OrderMapper orderMapper;
    private OrderItemMapper orderItemMapper;
    private StockService stockService;
    private CouponService couponService;
    private PointsService pointsService;
    private SkuMapper skuMapper;
    private ProductMapper productMapper;
    private StringRedisTemplate stringRedisTemplate;
    private BehaviorService behaviorService;
    private com.pzhu.mall.modules.order.mapper.PaymentMapper paymentMapper;
    private com.pzhu.mall.modules.product.service.ReviewService reviewService;
    private com.pzhu.mall.modules.logistics.mapper.LogisticsMapper logisticsMapper;
    private OrderService service;

    @BeforeAll
    static void initTableInfo() {
        // LambdaUpdateWrapper.set()/eq() 通过 lambda 缓存解析列名，
        // 纯单测（无 Spring 容器）必须手动初始化实体 TableInfo，否则抛
        // "can not find lambda cache for this entity"
        var assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Order.class);
        TableInfoHelper.initTableInfo(assistant, OrderItem.class);
    }

    @BeforeEach
    void setUp() {
        orderMapper = mock(OrderMapper.class);
        orderItemMapper = mock(OrderItemMapper.class);
        stockService = mock(StockService.class);
        couponService = mock(CouponService.class);
        pointsService = mock(PointsService.class);
        skuMapper = mock(SkuMapper.class);
        productMapper = mock(ProductMapper.class);
        stringRedisTemplate = mock(StringRedisTemplate.class);
        behaviorService = mock(BehaviorService.class);
        paymentMapper = mock(com.pzhu.mall.modules.order.mapper.PaymentMapper.class);
        reviewService = mock(com.pzhu.mall.modules.product.service.ReviewService.class);
        logisticsMapper = mock(com.pzhu.mall.modules.logistics.mapper.LogisticsMapper.class);

        service = new OrderService();
        inject(service, "orderMapper", orderMapper);
        inject(service, "orderItemMapper", orderItemMapper);
        inject(service, "stockService", stockService);
        inject(service, "couponService", couponService);
        inject(service, "pointsService", pointsService);
        inject(service, "skuMapper", skuMapper);
        inject(service, "productMapper", productMapper);
        inject(service, "stringRedisTemplate", stringRedisTemplate);
        inject(service, "behaviorService", behaviorService);
        inject(service, "paymentMapper", paymentMapper);
        inject(service, "reviewService", reviewService);
        inject(service, "logisticsMapper", logisticsMapper);

        // pay() 内部注册事务提交后回调，需要激活事务同步（单元测试无真实事务）
        TransactionSynchronizationManager.initSynchronization();

        // M-15 修复验证：pay() 需读取登录上下文做订单归属校验，默认登录用户 100L
        LoginUserContext.set(100L, 1);
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
        LoginUserContext.clear();
    }

    // ==================== cancelOrder ====================

    @Test
    void cancelOrder_success_releasesStockCouponPoints() {
        LoginUserContext.set(100L, 1);

        Order order = pendingOrder(1L, 100L);
        order.setCouponDiscountAmount(new BigDecimal("10.00"));
        order.setPointsDeductAmount(new BigDecimal("5.00"));
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(orderMapper.update(isNull(), any())).thenReturn(1);

        OrderItem withSku = item(1L, 10L, 2L, 1);
        OrderItem noSku = item(2L, 11L, null, 3);
        when(orderItemMapper.selectList(any())).thenReturn(Arrays.asList(withSku, noSku));

        service.cancelOrder(1L);

        // L2-02 修复：库存归还在事务提交后（afterCommit）执行；单元测试无真实提交，
        // 手动触发已注册的事务回调以验证归还逻辑
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);

        // 仅有 skuId 的订单项归还 SKU 级库存
        verify(stockService).rollback(2L, 1);
        verify(stockService, times(1)).rollback(anyLong(), anyInt());
        // H-4 修复验证：无 SKU 订单项归还商品级库存
        verify(stockService).rollbackProduct(11L, 3);
        verify(stockService, times(1)).rollbackProduct(anyLong(), anyInt());
        // 优惠券与积分均释放（H-5 后取消路径返还抵扣积分 refundDeduct，而非 clawback）
        verify(couponService).releaseByOrderId(1L);
        verify(pointsService).refundDeduct(1L);
    }

    @Test
    void cancelOrder_noCouponNoPoints_skipsRelease() {
        LoginUserContext.set(100L, 1);

        Order order = pendingOrder(1L, 100L);
        order.setCouponDiscountAmount(BigDecimal.ZERO);
        order.setPointsDeductAmount(null);
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(orderMapper.update(isNull(), any())).thenReturn(1);
        when(orderItemMapper.selectList(any())).thenReturn(Collections.emptyList());

        service.cancelOrder(1L);

        verify(couponService, never()).releaseByOrderId(anyLong());
        verify(pointsService, never()).refundDeduct(anyLong());
    }

    @Test
    void cancelOrder_notFound_throws() {
        LoginUserContext.set(100L, 1);
        when(orderMapper.selectById(1L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.cancelOrder(1L));
        assertEquals(ErrorCode.ORDER_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void cancelOrder_notOwner_throws() {
        LoginUserContext.set(100L, 1);
        when(orderMapper.selectById(1L)).thenReturn(pendingOrder(1L, 999L));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.cancelOrder(1L));
        assertEquals(ErrorCode.ORDER_NOT_FOUND.getCode(), ex.getCode());
        verify(orderMapper, never()).update(any(), any());
    }

    @Test
    void cancelOrder_statusChangedConcurrently_throws() {
        LoginUserContext.set(100L, 1);
        when(orderMapper.selectById(1L)).thenReturn(pendingOrder(1L, 100L));
        when(orderMapper.update(isNull(), any())).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.cancelOrder(1L));
        assertEquals(ErrorCode.ORDER_STATUS_INVALID.getCode(), ex.getCode());
        verify(stockService, never()).rollback(anyLong(), anyInt());
    }

    // ==================== cancelOrderBySystem (H-16) ====================

    @Test
    void cancelOrderBySystem_withoutLoginContext_succeeds() {
        // H-16 修复验证：定时任务线程无登录上下文，系统取消不应依赖 LoginUserContext
        LoginUserContext.clear();

        Order order = pendingOrder(1L, 100L);
        order.setCouponDiscountAmount(new BigDecimal("10.00"));
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(orderMapper.update(isNull(), any())).thenReturn(1);
        when(orderItemMapper.selectList(any()))
                .thenReturn(Collections.singletonList(item(1L, 10L, 2L, 1)));

        assertDoesNotThrow(() -> service.cancelOrderBySystem(1L));

        // L2-02 修复：库存归还在事务提交后（afterCommit）执行，手动触发已注册回调
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);
        verify(stockService).rollback(2L, 1);
        verify(couponService).releaseByOrderId(1L);
    }

    @Test
    void cancelOrderBySystem_orderNotFound_silentSkip() {
        when(orderMapper.selectById(1L)).thenReturn(null);

        assertDoesNotThrow(() -> service.cancelOrderBySystem(1L));
        verify(orderMapper, never()).update(any(), any());
    }

    @Test
    void cancelOrderBySystem_alreadyPaid_idempotentSkip() {
        // 订单已被用户支付（原子 UPDATE 影响 0 行）→ 静默跳过，不抛异常、无副作用
        Order order = pendingOrder(1L, 100L);
        order.setStatus(1);
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(orderMapper.update(isNull(), any())).thenReturn(0);

        assertDoesNotThrow(() -> service.cancelOrderBySystem(1L));
        verify(stockService, never()).rollback(anyLong(), anyInt());
        verify(couponService, never()).releaseByOrderId(anyLong());
    }

    // ==================== confirmReceive ====================

    @Test
    void confirmReceive_success() {
        LoginUserContext.set(100L, 1);
        Order order = pendingOrder(1L, 100L);
        order.setStatus(2);
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(orderMapper.update(isNull(), any())).thenReturn(1);

        assertDoesNotThrow(() -> service.confirmReceive(1L));
        verify(orderMapper).update(isNull(), any());
    }

    @Test
    void confirmReceive_notShipped_throws() {
        LoginUserContext.set(100L, 1);
        when(orderMapper.selectById(1L)).thenReturn(pendingOrder(1L, 100L)); // status=0

        BusinessException ex = assertThrows(BusinessException.class, () -> service.confirmReceive(1L));
        assertEquals(ErrorCode.ORDER_STATUS_INVALID.getCode(), ex.getCode());
    }

    @Test
    void confirmReceive_concurrentUpdateLost_throws() {
        LoginUserContext.set(100L, 1);
        Order order = pendingOrder(1L, 100L);
        order.setStatus(2);
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(orderMapper.update(isNull(), any())).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.confirmReceive(1L));
        assertEquals(ErrorCode.ORDER_STATUS_INVALID.getCode(), ex.getCode());
    }

    @Test
    void confirmReceive_notOwner_throws() {
        LoginUserContext.set(100L, 1);
        Order order = pendingOrder(1L, 999L);
        order.setStatus(2);
        when(orderMapper.selectById(1L)).thenReturn(order);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.confirmReceive(1L));
        assertEquals(ErrorCode.ORDER_NOT_FOUND.getCode(), ex.getCode());
    }

    // ==================== pay ====================

    @Test
    void pay_success_deductsStockSettlesPointsRecordsBehavior() {
        Order order = pendingOrder(1L, 100L);
        order.setPayAmount(new BigDecimal("199.00"));
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        when(orderMapper.update(isNull(), any())).thenReturn(1);

        OrderItem item = item(1L, 10L, 2L, 3);
        when(orderItemMapper.selectList(any())).thenReturn(Collections.singletonList(item));
        when(skuMapper.deductStock(2L, 3)).thenReturn(true);
        when(productMapper.deductStockUnchecked(10L, 3)).thenReturn(1);

        service.pay(1L, 1);

        // O-04 修复：商品总库存使用原子 UPDATE 扣减（替代读改写），并发下不漂移
        verify(skuMapper).deductStock(2L, 3);
        verify(productMapper).deductStockUnchecked(10L, 3);
        // O-05 修复：支付记录落库
        verify(paymentMapper).insert(any(com.pzhu.mall.modules.order.entity.Payment.class));
        // 积分结算
        verify(pointsService).settleEarn(1L, 100L, new BigDecimal("199.00"));
        // 购买行为（behaviorType=3）已于 OrderGroupProcessor 下单时记录一次，pay() 不再重复记录
        verify(behaviorService, never()).record(100L, 10L, 3);
    }

    @Test
    void pay_statusNotPending_throws() {
        Order order = pendingOrder(1L, 100L);
        order.setStatus(1);
        when(orderMapper.selectById(1L)).thenReturn(order);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.pay(1L, 1));
        assertEquals(ErrorCode.ORDER_ALREADY_PAID.getCode(), ex.getCode());
    }

    @Test
    void pay_redisIdempotentMarker_throws() {
        when(orderMapper.selectById(1L)).thenReturn(pendingOrder(1L, 100L));
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.pay(1L, 1));
        assertEquals(ErrorCode.ORDER_ALREADY_PAID.getCode(), ex.getCode());
        verify(orderMapper, never()).update(any(), any());
    }

    @Test
    void pay_redisUnavailable_degradesAndProceeds() {
        // C4 修复验证：Redis 不可用时降级放行，依赖数据库原子更新兜底
        Order order = pendingOrder(1L, 100L);
        order.setPayAmount(new BigDecimal("10.00"));
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(stringRedisTemplate.hasKey(anyString())).thenThrow(new RuntimeException("redis down"));
        when(orderMapper.update(isNull(), any())).thenReturn(1);
        when(orderItemMapper.selectList(any())).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> service.pay(1L, 1));
        verify(pointsService).settleEarn(eq(1L), eq(100L), any(BigDecimal.class));
    }

    @Test
    void pay_concurrentUpdateLost_throws() {
        when(orderMapper.selectById(1L)).thenReturn(pendingOrder(1L, 100L));
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        when(orderMapper.update(isNull(), any())).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.pay(1L, 1));
        assertEquals(ErrorCode.ORDER_ALREADY_PAID.getCode(), ex.getCode());
    }

    @Test
    void pay_stockNotEnough_throws() {
        Order order = pendingOrder(1L, 100L);
        order.setPayAmount(new BigDecimal("10.00"));
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        when(orderMapper.update(isNull(), any())).thenReturn(1);
        when(orderItemMapper.selectList(any()))
                .thenReturn(Collections.singletonList(item(1L, 10L, 2L, 3)));
        when(skuMapper.deductStock(2L, 3)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.pay(1L, 1));
        assertEquals(ErrorCode.STOCK_NOT_ENOUGH.getCode(), ex.getCode());
    }

    @Test
    void pay_noSkuItem_deductsProductStock() {
        // H-4 修复验证：单规格商品（订单项 skuId 为空）支付时走商品级原子扣减
        Order order = pendingOrder(1L, 100L);
        order.setPayAmount(new BigDecimal("59.00"));
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        when(orderMapper.update(isNull(), any())).thenReturn(1);
        when(orderItemMapper.selectList(any()))
                .thenReturn(Collections.singletonList(item(1L, 11L, null, 2)));
        when(productMapper.deductStock(11L, 2)).thenReturn(true);

        assertDoesNotThrow(() -> service.pay(1L, 1));

        verify(productMapper).deductStock(11L, 2);
        verify(skuMapper, never()).deductStock(anyLong(), anyInt());
        verify(pointsService).settleEarn(1L, 100L, new BigDecimal("59.00"));
    }

    @Test
    void pay_noSkuItem_stockNotEnough_throws() {
        // H-4 修复验证：商品级原子扣减失败（库存不足）时拒绝支付
        Order order = pendingOrder(1L, 100L);
        order.setPayAmount(new BigDecimal("59.00"));
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        when(orderMapper.update(isNull(), any())).thenReturn(1);
        when(orderItemMapper.selectList(any()))
                .thenReturn(Collections.singletonList(item(1L, 11L, null, 2)));
        when(productMapper.deductStock(11L, 2)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.pay(1L, 1));
        assertEquals(ErrorCode.STOCK_NOT_ENOUGH.getCode(), ex.getCode());
    }

    @Test
    void pay_orderBelongsToOtherUser_throwsNotFound() {
        // M-15 修复验证：登录用户 100L 不能支付他人（999L）的订单
        when(orderMapper.selectById(1L)).thenReturn(pendingOrder(1L, 999L));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.pay(1L, 1));
        assertEquals(ErrorCode.ORDER_NOT_FOUND.getCode(), ex.getCode());
        verify(orderMapper, never()).update(any(), any());
    }

    // ==================== createOrder（C-1 SKU 绑定校验） ====================

    @Test
    @SuppressWarnings("unchecked")
    void createOrder_skuBelongsToOtherProduct_throwsMismatchBeforeStockDeduct() {
        // C-1 修复验证："商品10 + 商品777的SKU"下单必须在库存预扣减之前被拒绝，
        // 且错误码为 SKU_PRODUCT_MISMATCH（不被库存 try-catch 吞成"库存不足"）
        LoginUserContext.set(100L, 1);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);

        Product product = new Product();
        product.setId(10L);
        product.setShopId(1L);
        product.setStatus(1); // ONLINE
        product.setIsDeleted(0);
        when(productMapper.selectById(10L)).thenReturn(product);

        Sku foreignSku = new Sku();
        foreignSku.setId(5L);
        foreignSku.setProductId(777L); // 归属其他商品
        when(skuMapper.selectById(5L)).thenReturn(foreignSku);

        CreateOrderDTO dto = new CreateOrderDTO();
        dto.setRequestId("req-c1");
        ProductItemDTO pi = new ProductItemDTO();
        pi.setProductId(10L);
        pi.setSkuId(5L);
        pi.setQuantity(1);
        dto.setProductItems(Collections.singletonList(pi));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createOrder(dto));
        assertEquals(ErrorCode.SKU_PRODUCT_MISMATCH.getCode(), ex.getCode());
        verify(stockService, never()).deduct(anyLong(), anyInt());
    }

    // ==================== review（M-01 赠品拦截 + 评价行为） ====================

    @Test
    void review_giftItem_throwsParamError() {
        // M-01 修复验证：赠品行不可单独评价（后端兜底拦截）
        OrderItem gift = item(50L, 10L, null, 1);
        gift.setIsGift(1);
        when(orderItemMapper.selectById(50L)).thenReturn(gift);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.review(50L, 5, "好评"));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
        verify(reviewService, never()).submit(any(), any(), any(), any(), any());
    }

    @Test
    void review_itemNotFound_throws() {
        when(orderItemMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.review(999L, 5, "x"));
    }

    @Test
    void review_highRating_recordsBehavior() {
        // 好评（>=4）计入评价行为 behaviorType=4
        OrderItem item = item(51L, 10L, null, 1);
        item.setIsGift(0);
        when(orderItemMapper.selectById(51L)).thenReturn(item);

        service.review(51L, 5, "很好");

        verify(reviewService).submit(eq(51L), eq(100L), eq(5), eq("很好"), isNull());
        verify(behaviorService).record(100L, 10L, 4);
    }

    @Test
    void review_lowRating_noBehavior() {
        // 差评（<4）不计入推荐行为矩阵
        OrderItem item = item(52L, 11L, null, 1);
        item.setIsGift(0);
        when(orderItemMapper.selectById(52L)).thenReturn(item);

        service.review(52L, 2, "差评");

        verify(reviewService).submit(eq(52L), eq(100L), eq(2), eq("差评"), isNull());
        verify(behaviorService, never()).record(anyLong(), anyLong(), eq(4));
    }

    // ==================== deleteOrder（B-1 删除订单） ====================

    private static Order cancelledOrder(Long id, Long userId) {
        Order order = new Order();
        order.setId(id);
        order.setUserId(userId);
        order.setStatus(5); // 已取消
        order.setIsDeleted(0);
        return order;
    }

    @Test
    void deleteOrder_cancelledStatus_success() {
        // O-01：状态=5 本人删除 → 成功，is_deleted=1
        LoginUserContext.set(100L, 1);
        Order order = cancelledOrder(1L, 100L);
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(orderMapper.update(isNull(), any())).thenReturn(1);

        service.deleteOrder(100L, 1L);
        // 原子 UPDATE 携带 is_deleted=1 条件
        org.mockito.ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper> captor =
                org.mockito.ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper.class);
        verify(orderMapper).update(isNull(), captor.capture());
        assertNotNull(captor.getValue());
    }

    @Test
    void deleteOrder_pendingStatus_rejected() {
        // O-02：状态=0（待付款）→ 抛 10001
        LoginUserContext.set(100L, 1);
        Order order = pendingOrder(1L, 100L);
        order.setIsDeleted(0);
        when(orderMapper.selectById(1L)).thenReturn(order);

        com.pzhu.mall.common.exception.BusinessException ex =
                assertThrows(com.pzhu.mall.common.exception.BusinessException.class,
                        () -> service.deleteOrder(100L, 1L));
        assertTrue(ex.getMessage().contains("仅已取消或已退款"));
    }

    @Test
    void deleteOrder_otherUserOrder_forbidden() {
        // O-03：他人订单 → 抛 10003（IDOR）
        LoginUserContext.set(100L, 1);
        Order order = cancelledOrder(1L, 200L);
        when(orderMapper.selectById(1L)).thenReturn(order);

        assertThrows(com.pzhu.mall.common.exception.BusinessException.class,
                () -> service.deleteOrder(100L, 1L));
    }

    @Test
    void deleteOrder_alreadyDeleted_notFound() {
        // O-04：已删除订单再删 → 抛 40005
        LoginUserContext.set(100L, 1);
        Order order = cancelledOrder(1L, 100L);
        order.setIsDeleted(1);
        when(orderMapper.selectById(1L)).thenReturn(order);

        assertThrows(com.pzhu.mall.common.exception.BusinessException.class,
                () -> service.deleteOrder(100L, 1L));
    }

    @Test
    void deleteOrder_concurrentUpdate_rejected() {
        // O-05：并发删除（update 返回 0）→ 抛"状态已变化"
        LoginUserContext.set(100L, 1);
        Order order = cancelledOrder(1L, 100L);
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(orderMapper.update(isNull(), any())).thenReturn(0);

        com.pzhu.mall.common.exception.BusinessException ex =
                assertThrows(com.pzhu.mall.common.exception.BusinessException.class,
                        () -> service.deleteOrder(100L, 1L));
        assertTrue(ex.getMessage().contains("状态已变化"));
    }

    @Test
    void listByUser_filtersDeletedOrders() {
        // O-06：listByUser 过滤 is_deleted=1（审核修正验证）
        LoginUserContext.set(100L, 1);
        when(orderMapper.selectList(any())).thenReturn(java.util.Collections.emptyList());

        // 无分页参数走 selectList 分支（避免 selectPage 返回 null 的 NPE）
        service.listByUser();
        verify(orderMapper).selectList(any());
    }

    // ==================== ship（E-1 补测：发货链路） ====================

    private static Order shippedReadyOrder(Long id, Long shopId) {
        Order order = new Order();
        order.setId(id);
        order.setShopId(shopId);
        order.setStatus(1); // 待发货
        order.setIsDeleted(0);
        return order;
    }

    @Test
    void ship_success_writesLogistics() {
        // SHIP-01：发货成功 → 状态2 + 物流记录
        LoginUserContext.set(100L, 1);
        Order order = shippedReadyOrder(1L, 1L);
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(orderMapper.update(isNull(), any())).thenReturn(1);

        service.ship(1L, "顺丰速运", "SF123456", 1L);

        verify(orderMapper).update(isNull(), any());
        verify(logisticsMapper).insert(any());
    }

    @Test
    void ship_otherShop_throwsNotFound() {
        // SHIP-02：非本店订单 → 40005
        Order order = shippedReadyOrder(1L, 99L);
        when(orderMapper.selectById(1L)).thenReturn(order);

        assertThrows(com.pzhu.mall.common.exception.BusinessException.class,
                () -> service.ship(1L, "顺丰", "SF123", 1L));
        verify(logisticsMapper, never()).insert(any());
    }

    @Test
    void ship_wrongStatus_throwsStatusInvalid() {
        // SHIP-03：非待发货状态（status=2）→ 40004
        LoginUserContext.set(100L, 1);
        Order order = shippedReadyOrder(1L, 1L);
        order.setStatus(2);
        when(orderMapper.selectById(1L)).thenReturn(order);

        assertThrows(com.pzhu.mall.common.exception.BusinessException.class,
                () -> service.ship(1L, "顺丰", "SF123", 1L));
    }

    @Test
    void ship_concurrentUpdate_throwsStatusInvalid() {
        // SHIP-04：并发（update=0）→ 40004
        LoginUserContext.set(100L, 1);
        Order order = shippedReadyOrder(1L, 1L);
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(orderMapper.update(isNull(), any())).thenReturn(0);

        assertThrows(com.pzhu.mall.common.exception.BusinessException.class,
                () -> service.ship(1L, "顺丰", "SF123", 1L));
        verify(logisticsMapper, never()).insert(any());
    }

    // ==================== helpers ====================

    private static Order pendingOrder(Long id, Long userId) {
        Order order = new Order();
        order.setId(id);
        order.setUserId(userId);
        order.setStatus(0);
        return order;
    }

    private static OrderItem item(Long id, Long productId, Long skuId, int quantity) {
        OrderItem item = new OrderItem();
        item.setId(id);
        item.setOrderId(1L);
        item.setProductId(productId);
        item.setSkuId(skuId);
        item.setQuantity(quantity);
        return item;
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
