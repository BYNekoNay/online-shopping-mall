package com.pzhu.mall;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.pzhu.mall.modules.behavior.service.BehaviorService;
import com.pzhu.mall.modules.cart.entity.Cart;
import com.pzhu.mall.modules.cart.service.CartService;
import com.pzhu.mall.modules.logistics.entity.Logistics;
import com.pzhu.mall.modules.logistics.mapper.LogisticsMapper;
import com.pzhu.mall.modules.logistics.service.FreightService;
import com.pzhu.mall.modules.marketing.entity.PointsRecord;
import com.pzhu.mall.modules.marketing.mapper.PointsRecordMapper;
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
import com.pzhu.mall.modules.order.service.OrderGroupProcessor;
import com.pzhu.mall.modules.order.service.OrderService;
import com.pzhu.mall.modules.order.vo.OrderVO;
import com.pzhu.mall.modules.product.entity.Product;
import com.pzhu.mall.modules.product.entity.Review;
import com.pzhu.mall.modules.product.mapper.ProductMapper;
import com.pzhu.mall.modules.product.mapper.ReviewMapper;
import com.pzhu.mall.modules.product.mapper.SkuMapper;
import com.pzhu.mall.modules.product.service.ReviewService;
import com.pzhu.mall.modules.cart.mapper.CartMapper;
import com.pzhu.mall.modules.user.entity.Address;
import com.pzhu.mall.modules.user.entity.User;
import com.pzhu.mall.modules.user.mapper.AddressMapper;
import com.pzhu.mall.modules.user.mapper.UserMapper;
import com.pzhu.mall.modules.user.service.AddressService;
import com.pzhu.mall.modules.user.service.LoginAttemptService;
import com.pzhu.mall.modules.user.service.UserService;
import com.pzhu.mall.security.JwtUtil;
import com.pzhu.mall.security.LoginUserContext;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * 6.6 主链路集成测试（服务编排级，可离线执行）。
 *
 * <p>用真实的 Service 实例 + Mock 持久层，串联消费者主链路：
 * 注册 → 登录 → 加购 → 下单 → 支付 → 商家发货 → 确认收货 → 评价。
 * 各步骤之间通过共享的 Mock 状态（订单实体、登录上下文）传递数据，
 * 验证跨服务的契约与状态机流转（0待付款 → 1待发货 → 2已发货 → 4已完成）。
 *
 * <p>与 {@link FullChainIntegrationTest} 的区别：后者是 HTTP 级端到端用例目录，
 * 需要本地 MySQL + Redis；本测试不依赖任何基础设施，随 {@code mvn test} 常规执行。
 */
class MainChainIntegrationTest {

    // ---- 共享 Mock 持久层 ----
    private UserMapper userMapper;
    private AddressMapper addressMapper;
    private CartMapper cartMapper;
    private ProductMapper productMapper;
    private SkuMapper skuMapper;
    private OrderMapper orderMapper;
    private OrderItemMapper orderItemMapper;
    private ReviewMapper reviewMapper;
    private PointsRecordMapper pointsRecordMapper;
    private LogisticsMapper logisticsMapper;
    private StringRedisTemplate stringRedisTemplate;
    private ValueOperations<String, String> valueOperations;

    // ---- 真实服务 ----
    private UserService userService;
    private CartService cartService;
    private OrderService orderService;
    private ReviewService reviewService;
    private PointsService pointsService;
    private JwtUtil jwtUtil;

    /** 模拟数据库中的订单行（跨步骤共享状态） */
    private Order dbOrder;

    @BeforeAll
    static void initTableInfo() {
        var assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Order.class);
        TableInfoHelper.initTableInfo(assistant, OrderItem.class);
        TableInfoHelper.initTableInfo(assistant, Cart.class);
        TableInfoHelper.initTableInfo(assistant, User.class);
        TableInfoHelper.initTableInfo(assistant, PointsRecord.class);
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        userMapper = mock(UserMapper.class);
        addressMapper = mock(AddressMapper.class);
        cartMapper = mock(CartMapper.class);
        productMapper = mock(ProductMapper.class);
        skuMapper = mock(SkuMapper.class);
        orderMapper = mock(OrderMapper.class);
        orderItemMapper = mock(OrderItemMapper.class);
        reviewMapper = mock(ReviewMapper.class);
        pointsRecordMapper = mock(PointsRecordMapper.class);
        logisticsMapper = mock(LogisticsMapper.class);
        stringRedisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        // JWT：真实组件 + 测试密钥
        jwtUtil = new JwtUtil();
        inject(jwtUtil, "secret", "test-secret-key-for-main-chain-0123456789");
        inject(jwtUtil, "expireSeconds", 604800L);
        jwtUtil.init();

        LoginAttemptService loginAttemptService = mock(LoginAttemptService.class);
        userService = new UserService(userMapper, mock(AddressService.class), jwtUtil, loginAttemptService);

        cartService = new CartService();
        inject(cartService, "cartMapper", cartMapper);
        inject(cartService, "productMapper", productMapper);
        inject(cartService, "skuMapper", skuMapper);

        pointsService = new PointsService();
        inject(pointsService, "userMapper", userMapper);
        inject(pointsService, "pointsRecordMapper", pointsRecordMapper);

        reviewService = new ReviewService();
        inject(reviewService, "reviewMapper", reviewMapper);
        inject(reviewService, "orderItemMapper", orderItemMapper);
        inject(reviewService, "orderMapper", orderMapper);

        // 订单分组处理器（独立事务逻辑在单测中已覆盖，此处直接调用）
        OrderGroupProcessor orderGroupProcessor = new OrderGroupProcessor();
        inject(orderGroupProcessor, "orderMapper", orderMapper);
        inject(orderGroupProcessor, "orderItemMapper", orderItemMapper);
        inject(orderGroupProcessor, "orderNoGenerator", new OrderNoGenerator());
        inject(orderGroupProcessor, "productMapper", productMapper);
        inject(orderGroupProcessor, "skuMapper", skuMapper);
        FreightService freightService = mock(FreightService.class);
        when(freightService.calculate(anyLong(), any(), any())).thenReturn(new BigDecimal("10.00"));
        inject(orderGroupProcessor, "freightService", freightService);
        PromotionService promotionService = mock(PromotionService.class);
        when(promotionService.calculateDiscount(anyLong(), any())).thenReturn(BigDecimal.ZERO);
        inject(orderGroupProcessor, "promotionService", promotionService);
        inject(orderGroupProcessor, "pointsService", pointsService);
        inject(orderGroupProcessor, "couponService", mock(CouponService.class));
        inject(orderGroupProcessor, "behaviorService", mock(BehaviorService.class));
        inject(orderGroupProcessor, "cartMapper", cartMapper);

        orderService = new OrderService();
        inject(orderService, "orderNoGenerator", new OrderNoGenerator());
        // H-4 修复适配：链测商品为单规格商品（skuId=null），预扣减走 deductProduct，
        // 未 stub 的 mock 默认返回 false 会被判"库存不足"，故预置扣减成功
        StockService stockService = mock(StockService.class);
        when(stockService.deduct(anyLong(), anyInt())).thenReturn(true);
        when(stockService.deductProduct(anyLong(), anyInt())).thenReturn(true);
        inject(orderService, "stockService", stockService);
        inject(orderService, "stringRedisTemplate", stringRedisTemplate);
        inject(orderService, "orderMapper", orderMapper);
        inject(orderService, "orderItemMapper", orderItemMapper);
        inject(orderService, "cartMapper", cartMapper);
        inject(orderService, "productMapper", productMapper);
        inject(orderService, "skuMapper", skuMapper);
        inject(orderService, "reviewService", reviewService);
        inject(orderService, "addressMapper", addressMapper);
        inject(orderService, "couponService", mock(CouponService.class));
        inject(orderService, "promotionService", promotionService);
        inject(orderService, "pointsService", pointsService);
        inject(orderService, "behaviorService", mock(BehaviorService.class));
        inject(orderService, "freightService", freightService);
        inject(orderService, "logisticsMapper", logisticsMapper);
        inject(orderService, "orderGroupProcessor", orderGroupProcessor);
        // O-05 修复适配：支付落库 payment 记录
        inject(orderService, "paymentMapper", mock(com.pzhu.mall.modules.order.mapper.PaymentMapper.class));

        // pay() 内部注册事务提交后回调，需要激活事务同步
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
        LoginUserContext.clear();
    }

    @Test
    void mainChain_register_cart_order_pay_ship_receive_review() {
        // ==================== 1. 注册 ====================
        when(userMapper.selectCount(any())).thenReturn(0L);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userMapper.insert(userCaptor.capture())).thenAnswer(inv -> {
            inv.getArgument(0, User.class).setId(100L);
            return 1;
        });

        Long userId = userService.register("chainuser", "Test12345", null, null, null);
        assertEquals(100L, userId);
        User registered = userCaptor.getValue();
        assertNotEquals("Test12345", registered.getPassword());
        assertTrue(registered.getPassword().startsWith("$2"), "密码必须 BCrypt 加密存储");
        assertEquals(1, registered.getRole());

        // ==================== 2. 登录 ====================
        when(userMapper.selectOne(any())).thenReturn(registered);
        String token = userService.login("chainuser", "Test12345");
        assertNotNull(token);
        assertEquals("100", jwtUtil.parseToken(token).getSubject());
        // 模拟 JwtInterceptor 写入登录上下文
        LoginUserContext.set(100L, 1);

        // ==================== 3. 加购 ====================
        when(productMapper.selectById(10L)).thenReturn(product());
        when(cartMapper.update(isNull(), any())).thenReturn(0);
        when(cartMapper.insert(any(Cart.class))).thenReturn(1);

        Cart cartReq = new Cart();
        cartReq.setProductId(10L);
        cartReq.setQuantity(2);
        cartService.add(cartReq);
        verify(cartMapper).insert(any(Cart.class));

        // ==================== 4. 下单 ====================
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);
        when(addressMapper.selectById(1L)).thenReturn(address());
        when(orderMapper.insert(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0, Order.class);
            o.setId(55L);
            dbOrder = o; // 记录"数据库"中的订单
            return 1;
        });
        when(orderItemMapper.insert(any(OrderItem.class))).thenAnswer(inv -> {
            inv.getArgument(0, OrderItem.class).setId(300L);
            return 1;
        });
        when(orderItemMapper.selectList(any()))
                .thenReturn(Collections.singletonList(orderItem(300L, 55L)));

        CreateOrderDTO dto = new CreateOrderDTO();
        dto.setRequestId("chain-req-001");
        dto.setAddressId(1L);
        ProductItemDTO pi = new ProductItemDTO();
        pi.setProductId(10L);
        pi.setQuantity(2);
        dto.setProductItems(Collections.singletonList(pi));
        dto.setUsePoints(false);

        List<OrderVO> orders = orderService.createOrder(dto);

        assertEquals(1, orders.size());
        OrderVO created = orders.get(0);
        assertEquals(55L, created.getOrderId());
        assertEquals(0, created.getStatus()); // 待付款
        // 商品额 100×2 + 运费 10 = 210
        assertEquals(0, new BigDecimal("210.00").compareTo(created.getPayAmount()));
        assertNotNull(created.getOrderNo());

        // ==================== 5. 支付 ====================
        when(orderMapper.selectById(55L)).thenReturn(dbOrder);
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        // H-4 修复适配：单规格商品支付走商品级原子扣减
        when(productMapper.deductStock(10L, 2)).thenReturn(true);
        when(orderMapper.update(isNull(), any())).thenAnswer(inv -> {
            dbOrder.setStatus(1); // 模拟原子 UPDATE 成功：待付款 → 待发货
            return 1;
        });
        when(userMapper.update(isNull(), any())).thenReturn(1);
        when(pointsRecordMapper.insert(any(PointsRecord.class))).thenReturn(1);

        orderService.pay(55L, 2);

        assertEquals(1, dbOrder.getStatus());
        ArgumentCaptor<PointsRecord> pointsCaptor = ArgumentCaptor.forClass(PointsRecord.class);
        verify(pointsRecordMapper).insert(pointsCaptor.capture());
        assertEquals(210, pointsCaptor.getValue().getChangeAmount()); // 实付 1:1 发放积分

        // ==================== 6. 商家发货 ====================
        when(orderMapper.update(isNull(), any())).thenAnswer(inv -> {
            dbOrder.setStatus(2); // 待发货 → 已发货
            return 1;
        });
        when(logisticsMapper.insert(any(Logistics.class))).thenReturn(1);
        orderService.ship(55L, "顺丰速运", "SF1234567890", 8L);
        assertEquals(2, dbOrder.getStatus()); // 已发货
        verify(logisticsMapper).insert(any(Logistics.class));

        // ==================== 7. 确认收货 ====================
        when(orderMapper.update(isNull(), any())).thenAnswer(inv -> {
            dbOrder.setStatus(4); // 已发货 → 已完成
            return 1;
        });
        orderService.confirmReceive(55L);
        assertEquals(4, dbOrder.getStatus()); // 已完成

        // ==================== 8. 评价 ====================
        when(orderItemMapper.selectById(300L)).thenReturn(orderItem(300L, 55L));
        when(reviewMapper.selectCount(any())).thenReturn(0L);
        when(reviewMapper.insert(any(Review.class))).thenReturn(1);

        orderService.review(300L, 5, "好评");

        ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
        verify(reviewMapper).insert(reviewCaptor.capture());
        assertEquals(100L, reviewCaptor.getValue().getUserId());
        assertEquals(5, reviewCaptor.getValue().getRating());
    }

    // ==================== fixtures ====================

    private static Product product() {
        Product p = new Product();
        p.setId(10L);
        p.setShopId(8L);
        p.setName("链测商品");
        p.setMainImage("https://example.com/p.jpg");
        p.setPrice(new BigDecimal("100.00"));
        p.setStock(50);
        p.setStatus(1);
        p.setIsDeleted(0);
        return p;
    }

    private static Address address() {
        Address a = new Address();
        a.setId(1L);
        a.setUserId(100L);
        a.setReceiver("链测用户");
        a.setPhone("13800138000");
        a.setProvince("四川省");
        a.setCity("攀枝花市");
        a.setDistrict("东区");
        a.setDetail("攀枝花学院");
        return a;
    }

    private static OrderItem orderItem(Long id, Long orderId) {
        OrderItem oi = new OrderItem();
        oi.setId(id);
        oi.setOrderId(orderId);
        oi.setProductId(10L);
        oi.setSkuId(null);
        oi.setProductNameSnapshot("链测商品");
        oi.setPrice(new BigDecimal("100.00"));
        oi.setQuantity(2);
        oi.setIsGift(0);
        return oi;
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
