package com.pzhu.mall.modules.order.service;

import com.pzhu.mall.common.config.RedisKeyPrefix;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.modules.cart.entity.Cart;
import com.pzhu.mall.modules.cart.mapper.CartMapper;
import com.pzhu.mall.modules.behavior.service.BehaviorService;
import com.pzhu.mall.modules.marketing.service.CouponService;
import com.pzhu.mall.modules.order.dto.CreateOrderDTO;
import com.pzhu.mall.modules.order.dto.ProductItemDTO;
import com.pzhu.mall.modules.order.entity.Order;
import com.pzhu.mall.modules.order.entity.OrderItem;
import com.pzhu.mall.modules.order.mapper.OrderMapper;
import com.pzhu.mall.modules.order.mapper.OrderItemMapper;
import com.pzhu.mall.modules.order.mapper.PaymentMapper;
import com.pzhu.mall.modules.order.component.OrderNoGenerator;
import com.pzhu.mall.modules.order.component.StockService;
import com.pzhu.mall.modules.order.vo.OrderItemVO;
import com.pzhu.mall.modules.order.vo.OrderVO;
import com.pzhu.mall.modules.product.service.ReviewService;
import com.pzhu.mall.modules.product.entity.Review;
import com.pzhu.mall.modules.product.entity.Product;
import com.pzhu.mall.modules.product.entity.Sku;
import com.pzhu.mall.modules.product.mapper.ProductMapper;
import com.pzhu.mall.modules.product.mapper.SkuMapper;
import com.pzhu.mall.modules.user.entity.Address;
import com.pzhu.mall.modules.user.mapper.AddressMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 订单服务。
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    @Resource
    private OrderNoGenerator orderNoGenerator;

    @Resource
    private StockService stockService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final Map<Integer, String> STATUS_MAP = Map.of(
        0, "待付款", 1, "待发货", 2, "已发货", 3, "已收货",
        4, "已完成", 5, "已取消", 6, "退款中", 7, "已退款"
    );

    /** 幂等 key 过期时间（24 小时，覆盖订单超时取消窗口） */
    private static final long IDEMPOTENT_TTL_HOURS = 24;

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private OrderItemMapper orderItemMapper;

    @Resource
    private PaymentMapper paymentMapper;

    @Resource
    private CartMapper cartMapper;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private SkuMapper skuMapper;

    @Resource
    private ReviewService reviewService;

    @Resource
    private AddressMapper addressMapper;

    @Resource
    private CouponService couponService;

    @Resource
    private com.pzhu.mall.modules.marketing.service.PromotionService promotionService;

    @Resource
    private com.pzhu.mall.modules.marketing.service.PointsService pointsService;

    @Resource
    private BehaviorService behaviorService;

    @Resource
    private com.pzhu.mall.modules.logistics.service.FreightService freightService;

    @Resource
    private com.pzhu.mall.modules.logistics.mapper.LogisticsMapper logisticsMapper;

    @Resource
    private OrderGroupProcessor orderGroupProcessor;

    /**
     * 提交订单（核心链路）。
     * <p>
     * 按店铺拆单后每组独立执行，每组通过 {@link OrderGroupProcessor#processGroup} 在独立事务中处理，
     * 单组因库存不足等原因失败时，已成功创建的子订单不回滚（符合设计文档要求）。
     * 每组内部通过 Redis 预扣减 + 失败手动回滚保证一致性。
     * <p>
     * 注意：本方法不标注 {@code @Transactional}，因为：
     * <ul>
     *   <li>Redis 库存预扣减操作不应在数据库事务范围内（违反 §10 "Redis 操作放在事务外"）；</li>
     *   <li>每组已由 {@code OrderGroupProcessor} 在自己的数据库事务中独立执行；</li>
     *   <li>购物车删除等操作不阻塞下单事务。</li>
     * </ul>
     */
    public List<OrderVO> createOrder(CreateOrderDTO dto) {
        Long userId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();

        // 0. 校验幂等请求 ID 存在（O-08 修复：SET NX 写入延后至全部入参校验通过后，
        // 避免校验失败也占用 24h 幂等键导致用户无法重试）
        String requestId = dto.getRequestId();
        if (requestId == null || requestId.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "requestId 不能为空");
        }

        // 1. 获取商品列表
        List<ProductItemDTO> items = dto.getProductItems();
        if (items == null || items.isEmpty()) {
            // 从购物车读取（校验归属：只读取当前用户的购物车项）
            List<Cart> carts = cartMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Cart>()
                    .in(Cart::getId, dto.getCartItemIds())
                    .eq(Cart::getUserId, userId)
            );
            items = carts.stream().map(c -> {
                ProductItemDTO p = new ProductItemDTO();
                p.setProductId(c.getProductId());
                p.setSkuId(c.getSkuId());
                p.setQuantity(c.getQuantity());
                // H-3/M-8 修复：记录来源购物车项 ID，便于按成功分组精确清理
                p.setCartId(c.getId());
                return p;
            }).collect(Collectors.toList());
        }

        if (items.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }

        // 2. 加载商品/店铺信息并按 shopId 分组
        Map<Long, List<ProductItemDTO>> byShop = new LinkedHashMap<>();
        for (ProductItemDTO item : items) {
            Product product = productMapper.selectById(item.getProductId());
            if (product == null || product.getIsDeleted() == 1) {
                throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
            }
            if (com.pzhu.mall.common.enums.ProductStatus.of(product.getStatus()) != com.pzhu.mall.common.enums.ProductStatus.ONLINE) {
                throw new BusinessException(ErrorCode.PRODUCT_OFFLINE_ORDER);
            }
            // C-1 修复：校验 SKU 与商品的绑定关系，防止用其他商品的低价 SKU 下单（价格篡改）；
            // 在库存预扣减之前快速失败，避免非法组合被库存扣减的 try-catch 吞成"库存不足"
            if (item.getSkuId() != null) {
                Sku sku = skuMapper.selectById(item.getSkuId());
                if (sku == null) {
                    throw new BusinessException(ErrorCode.SKU_NOT_FOUND);
                }
                if (!item.getProductId().equals(sku.getProductId())) {
                    throw new BusinessException(ErrorCode.SKU_PRODUCT_MISMATCH);
                }
            }
            byShop.computeIfAbsent(product.getShopId(), k -> new ArrayList<>()).add(item);
        }

        // 3. 获取收货地址
        Address address = addressMapper.selectById(dto.getAddressId());
        if (address == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        // M-01 修复：校验收货地址归属，防止越权使用他人地址下单；
        // 不满足时统一按"不存在"返回，避免暴露他人地址 ID 是否存在
        if (address.getUserId() == null || !address.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        // O-11 修复：地址快照改用 Jackson 序列化（原手拼 JSON 在 receiver/phone 含引号时破坏结构）
        String addressSnapshot;
        try {
            java.util.Map<String, String> addrMap = new LinkedHashMap<>();
            addrMap.put("receiver", address.getReceiver());
            addrMap.put("phone", address.getPhone());
            addrMap.put("province", address.getProvince());
            addrMap.put("city", address.getCity());
            addrMap.put("district", address.getDistrict());
            addrMap.put("detail", address.getDetail());
            addressSnapshot = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(addrMap);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_BUSY, "地址信息序列化失败");
        }

        // O-08 修复：所有入参校验通过后才写入幂等键（SET NX 24h），
        // 校验失败路径不占用幂等键，用户可立即修正后重试
        String idempotentKey = RedisKeyPrefix.ORDER + ":idempotent:" + requestId;
        Boolean alreadySet = stringRedisTemplate.opsForValue().setIfAbsent(idempotentKey, "1", IDEMPOTENT_TTL_HOURS, java.util.concurrent.TimeUnit.HOURS);
        if (alreadySet != null && !alreadySet) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单已提交，请勿重复操作");
        }

        List<OrderVO> allOrders = new ArrayList<>();
        boolean pointsProcessed = false;
        // H-6 修复：优惠券为单一资源，跨分组仅核销一次（与积分同模式）
        boolean couponProcessed = false;
        List<String> failedShops = new ArrayList<>();

        // 4. 按分组创建订单（每组独立事务，单组失败跳过并记录，其余组继续）
        for (Map.Entry<Long, List<ProductItemDTO>> entry : byShop.entrySet()) {
            Long shopId = entry.getKey();
            List<ProductItemDTO> groupItems = entry.getValue();

            List<Long> deductedSkus = new ArrayList<>();
            List<Integer> deductedQtys = new ArrayList<>();
            // H-4 修复：无 SKU 商品（单规格，直接用 product.stock）走商品级库存命名空间，
            // 与 SKU 级扣减分开跟踪，回滚时各自归还
            List<Long> deductedProducts = new ArrayList<>();
            List<Integer> deductedProductQtys = new ArrayList<>();
            boolean stockFailed = false;
            try {
                // 4.7 库存预扣减（该分组内全部商品，失败则回滚该分组已扣减的库存）
                for (ProductItemDTO item : groupItems) {
                    Sku sku = item.getSkuId() != null ? skuMapper.selectById(item.getSkuId()) : null;
                    if (sku != null) {
                        boolean ok = stockService.deduct(sku.getId(), item.getQuantity());
                        if (!ok) {
                            stockFailed = true;
                            break;
                        }
                        deductedSkus.add(sku.getId());
                        deductedQtys.add(item.getQuantity());
                    } else {
                        // H-4 修复：单规格商品扣减商品级库存
                        boolean ok = stockService.deductProduct(item.getProductId(), item.getQuantity());
                        if (!ok) {
                            stockFailed = true;
                            break;
                        }
                        deductedProducts.add(item.getProductId());
                        deductedProductQtys.add(item.getQuantity());
                    }
                }
            } catch (Exception e) {
                // O-03 修复：记录异常栈，避免库存预扣失败根因不可查
                log.error("[订单] 库存预扣减异常 shopId={} groupSize={}", shopId, groupItems.size(), e);
                stockFailed = true;
            }
            if (stockFailed) {
                for (int i = 0; i < deductedSkus.size(); i++) {
                    stockService.rollback(deductedSkus.get(i), deductedQtys.get(i));
                }
                for (int i = 0; i < deductedProducts.size(); i++) {
                    stockService.rollbackProduct(deductedProducts.get(i), deductedProductQtys.get(i));
                }
                failedShops.add("店铺" + shopId + "库存不足");
                continue;
            }

            // 4.8 调用独立事务方法处理该分组（insert order + orderItems + 积分/优惠券 + 购物车清理）
            boolean applyPoints = !pointsProcessed && Boolean.TRUE.equals(dto.getUsePoints());
            // H-6 修复：优惠券仅在首个成功分组计价并核销，后续分组不再触碰券状态
            boolean applyCoupon = !couponProcessed && dto.getUserCouponId() != null;
            // M-8 修复：仅清理本成功分组来源的购物车项（按 cartId 精确匹配），
            // 失败分组的商品保留在购物车，不再"部分失败删全部"
            List<Long> groupCartIds = groupItems.stream()
                    .map(ProductItemDTO::getCartId)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
            List<OrderVO> groupOrders;
            try {
                groupOrders = orderGroupProcessor.processGroup(userId, shopId, groupItems, addressSnapshot, address.getProvince(), dto, applyPoints,
                        applyCoupon, groupCartIds.isEmpty() ? null : groupCartIds);
            } catch (Exception e) {
                // 事务回滚后，归还 Redis 预扣减的库存
                for (int i = 0; i < deductedSkus.size(); i++) {
                    stockService.rollback(deductedSkus.get(i), deductedQtys.get(i));
                }
                // H-4 修复：同步归还商品级预扣库存
                for (int i = 0; i < deductedProducts.size(); i++) {
                    stockService.rollbackProduct(deductedProducts.get(i), deductedProductQtys.get(i));
                }
                // O-03 修复：记录异常栈，避免 processGroup 失败根因不可查（此前仅记"店铺xx下单失败"）
                log.error("[订单] 分组下单异常 shopId={} groupSize={}", shopId, groupItems.size(), e);
                // DV-02 动态验证修复：透传业务异常信息（如"商品数量必须大于0"），
                // 避免全失败时用户只看到笼统的"店铺X下单失败"
                String reason = (e instanceof BusinessException) ? ((BusinessException) e).getMessage() : "下单失败";
                failedShops.add("店铺" + shopId + reason);
                continue;
            }
            allOrders.addAll(groupOrders);

            // 标记积分已处理（仅第一个成功分组处理一次）
            if (applyPoints) {
                pointsProcessed = true;
            }
            // H-6 修复：标记优惠券已核销（仅第一个成功分组核销一次）
            if (applyCoupon) {
                couponProcessed = true;
            }
        }

        // 汇总部分失败的提示（C5 修复：部分失败返回成功订单+记录警告，不抛异常隐藏成功订单）
        if (!failedShops.isEmpty()) {
            log.warn("订单部分失败: {}", String.join("；", failedShops));
        }
        // DV-01 动态验证修复：全部分组均失败时必须报错，禁止返回"成功+空订单"。
        // 此前库存不足时 failedShops 累积后 continue，最终 allOrders 为空仍返回 code:0，
        // 前端会把下单失败误判为成功（无订单可支付），严重误导用户。
        if (allOrders.isEmpty() && !failedShops.isEmpty()) {
            throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH,
                    "下单失败：" + String.join("；", failedShops));
        }

        return allOrders;
    }

    /**
     * 获取订单详情。
     */
    public OrderVO getDetail(Long orderId) {
        Long userId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        return toVO(order);
    }

    /**
     * 获取当前用户的订单列表。
     * <p>O-07 修复：支持分页（pageNum/pageSize 可选，缺省返回全量以兼容既有前端）。</p>
     */
    public List<OrderVO> listByUser(Integer pageNum, Integer pageSize) {
        Long userId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();
        var qw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                // B-1 审核修正：过滤已删除订单（deleteOrder 软删后不显示）
                .eq(Order::getIsDeleted, 0)
                .orderByDesc(Order::getCreateTime);
        List<Order> orders;
        if (pageNum != null && pageSize != null && pageSize > 0) {
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<Order> page =
                    new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize);
            orders = orderMapper.selectPage(page, qw).getRecords();
        } else {
            orders = orderMapper.selectList(qw);
        }
        return toVOList(orders);
    }

    /**
     * 兼容旧调用（无分页参数）。
     */
    public List<OrderVO> listByUser() {
        return listByUser(null, null);
    }

    /**
     * 商家端：获取本店铺的订单列表。
     */
    public List<OrderVO> listByMerchant(Long shopId, Integer status) {
        var qw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order>();
        qw.eq(Order::getShopId, shopId)
          .eq(Order::getIsDeleted, 0);
        if (status != null) {
            qw.eq(Order::getStatus, status);
        }
        qw.orderByDesc(Order::getCreateTime);
        List<Order> orders = orderMapper.selectList(qw);
        return toVOList(orders);
    }

    /**
     * 商家端：发货（校验订单店铺归属，原子更新防并发）。
     */
    @Transactional
    public void ship(Long orderId, String logisticsCompany, String trackingNo, Long shopId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getShopId().equals(shopId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != 1) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }

        // M5 修复：原子更新，WHERE status=1 防重复发货
        boolean updated = orderMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Order>()
                        .set(Order::getStatus, 2)
                        .eq(Order::getId, orderId)
                        .eq(Order::getStatus, 1)
        ) > 0;
        if (!updated) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }

        // 写入物流记录
        com.pzhu.mall.modules.logistics.entity.Logistics logistics = new com.pzhu.mall.modules.logistics.entity.Logistics();
        logistics.setOrderId(orderId);
        logistics.setCompany(logisticsCompany);
        logistics.setTrackingNo(trackingNo);
        logistics.setStatus(1); // 待揽收
        logistics.setLastTrackInfo("商家已发货，等待快递揽收");
        logistics.setCreateTime(java.time.LocalDateTime.now());
        logistics.setUpdateTime(java.time.LocalDateTime.now());
        logisticsMapper.insert(logistics);
    }

    /**
     * 商家端：获取指定订单详情（校验店铺归属）。
     */
    public OrderVO getMerchantOrderDetail(Long orderId, Long shopId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getShopId().equals(shopId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        return toVO(order);
    }

    /**
     * 取消订单（用户主动取消）。
     * <p>使用原子 UPDATE（WHERE status=0）防止 TOCTOU 竞态条件，确保并发安全。</p>
     */
    @Transactional
    public void cancelOrder(Long orderId) {
        Long userId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();

        // 先读取订单（用于校验归属及后续优惠券/积分处理）
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        boolean updated = doCancel(order);
        if (!updated) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
    }

    /**
     * H-16 修复：系统级取消订单（供超时定时任务调用）。
     * <p>定时任务线程没有登录上下文（LoginUserContext 为 null），不能复用 cancelOrder 的
     * 用户归属校验（equals(null) 恒为 false 会导致超时取消永远失败）。
     * 幂等设计：订单不存在或已不处于"待付款"状态时静默跳过，不抛异常。</p>
     */
    @Transactional
    public void cancelOrderBySystem(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return;
        }
        doCancel(order);
    }

    /**
     * B-1 删除订单（软删除，任务书"订单管理-删除订单"）。
     *
     * <p>仅已取消(5)/已退款(7) 可删；软删 is_deleted=1 保留流水供对账；
     * 不释放库存/券/积分（已完成或已取消订单资产已结算）。</p>
     * <p>IDOR 防护：仅订单本人可删；原子 UPDATE（WHERE is_deleted=0）防重复删除。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || Integer.valueOf(1).equals(order.getIsDeleted())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            // IDOR 防护：非本人订单
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作该订单");
        }
        if (order.getStatus() != 5 && order.getStatus() != 7) {
            // 审核修正：ErrorCode 无 ORDER_STATUS_ERROR，统一 PARAM_ERROR
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅已取消或已退款的订单可删除");
        }
        boolean updated = orderMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Order>()
                        .set(Order::getIsDeleted, 1)
                        .eq(Order::getId, orderId)
                        .eq(Order::getIsDeleted, 0)
        ) > 0;
        if (!updated) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单状态已变化，请刷新后重试");
        }
        log.info("[订单] 用户={} 删除订单={}", userId, orderId);
    }

    /**
     * 取消订单公共逻辑：原子状态更新 + 库存归还 + 优惠券释放 + 积分扣回。
     *
     * @return true 取消成功；false 订单已不处于待付款状态（被并发处理），调用方自行决定语义
     */
    private boolean doCancel(Order order) {
        Long orderId = order.getId();

        // 原子更新：只在 status=0（待付款）时更新为 status=5（已取消）
        boolean updated = orderMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Order>()
                        .set(Order::getStatus, 5)
                        .eq(Order::getId, orderId)
                        .eq(Order::getStatus, 0)
        ) > 0;

        if (!updated) {
            return false;
        }

        // 库存归还（L2-02 修复：Redis 归还移至事务提交后执行，避免事务回滚时
        // Redis 库存已归还而 DB 订单未取消，造成可售库存虚增；与支付幂等标记的 afterCommit 模式一致）
        var itemQw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.pzhu.mall.modules.order.entity.OrderItem>();
        itemQw.eq(com.pzhu.mall.modules.order.entity.OrderItem::getOrderId, orderId);
        List<com.pzhu.mall.modules.order.entity.OrderItem> items = orderItemMapper.selectList(itemQw);
        List<Object[]> skuRollbacks = new ArrayList<>();
        List<Object[]> productRollbacks = new ArrayList<>();
        for (com.pzhu.mall.modules.order.entity.OrderItem item : items) {
            if (item.getSkuId() != null) {
                skuRollbacks.add(new Object[]{item.getSkuId(), item.getQuantity()});
            } else {
                // H-4 修复：单规格商品归还商品级库存
                productRollbacks.add(new Object[]{item.getProductId(), item.getQuantity()});
            }
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && (!skuRollbacks.isEmpty() || !productRollbacks.isEmpty())) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    for (Object[] r : skuRollbacks) {
                        try {
                            stockService.rollback((Long) r[0], (Integer) r[1]);
                        } catch (Exception e) {
                            log.error("[订单] 提交后 SKU 库存归还失败 orderId={} skuId={} qty={}（需人工对账）", orderId, r[0], r[1], e);
                        }
                    }
                    for (Object[] r : productRollbacks) {
                        try {
                            stockService.rollbackProduct((Long) r[0], (Integer) r[1]);
                        } catch (Exception e) {
                            log.error("[订单] 提交后商品库存归还失败 orderId={} productId={} qty={}（需人工对账）", orderId, r[0], r[1], e);
                        }
                    }
                }
            });
        } else {
            // 无活动事务（防御分支，正常不会走到）：直接归还
            for (Object[] r : skuRollbacks) {
                stockService.rollback((Long) r[0], (Integer) r[1]);
            }
            for (Object[] r : productRollbacks) {
                stockService.rollbackProduct((Long) r[0], (Integer) r[1]);
            }
        }

        // 释放优惠券（检查 couponDiscountAmount 而非 discountAmount，后者包含促销折扣和积分抵扣）
        if (order.getCouponDiscountAmount() != null && order.getCouponDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            couponService.releaseByOrderId(orderId);
        }

        // H-5 修复：未支付订单取消时返还下单抵扣的积分（type=2）。
        // 原实现调用 clawback（仅处理 type=1 获取记录），未支付订单无获取记录，抵扣积分永久丢失
        if (order.getPointsDeductAmount() != null && order.getPointsDeductAmount().compareTo(BigDecimal.ZERO) > 0) {
            pointsService.refundDeduct(orderId);
        }
        return true;
    }

    /**
     * 确认收货（原子更新，仅允许从"已发货"状态确认）。
     */
    @Transactional
    public void confirmReceive(Long orderId) {
        Long userId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != 2) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }

        // M6 修复：原子更新，WHERE status=2 防止跳过中间状态
        boolean updated = orderMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Order>()
                        .set(Order::getStatus, 4)
                        .eq(Order::getId, orderId)
                        .eq(Order::getStatus, 2)
        ) > 0;
        if (!updated) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
    }

    /**
     * 支付订单（模拟）。
     */
    @Transactional
    public void pay(Long orderId, Integer payType) {
        // O-15 修复：payType 仅支持 1（余额）/2（模拟支付宝）
        if (payType == null || (payType != 1 && payType != 2)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "支付方式仅支持 1=余额/2=模拟支付宝");
        }
        Long userId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();
        Order order = orderMapper.selectById(orderId);
        // M-15 修复：校验订单归属，防止越权支付他人订单
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != 0) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_PAID);
        }

        // 幂等快速失败：事务内检查 Redis 是否已标记已支付（C4 修复：Redis 不可用时降级放行）
        String payKey = RedisKeyPrefix.ORDER + ":paid:" + orderId;
        Boolean alreadyPaid;
        try {
            alreadyPaid = stringRedisTemplate.hasKey(payKey);
        } catch (Exception e) {
            log.warn("Redis unavailable during payment idempotency check, proceeding with DB guard", e);
            alreadyPaid = false;
        }
        if (Boolean.TRUE.equals(alreadyPaid)) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_PAID);
        }

        // 原子更新订单状态（C2 修复：WHERE status=0 防 pay/cancel 竞态）
        boolean updated = orderMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Order>()
                        .set(Order::getStatus, 1)
                        .set(Order::getPayType, payType)
                        .set(Order::getPayTime, LocalDateTime.now())
                        .eq(Order::getId, orderId)
                        .eq(Order::getStatus, 0)
        ) > 0;
        if (!updated) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_PAID);
        }

        // 事务提交后设置 Redis 幂等标记（C1 修复：避免事务回滚后标记残留）
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    stringRedisTemplate.opsForValue().setIfAbsent(payKey, "1", 30, java.util.concurrent.TimeUnit.DAYS);
                } catch (Exception e) {
                    log.error("Failed to set Redis payment marker after commit, orderId={}", orderId, e);
                }
            }
        });

        // 真实库存扣减（数据库原子操作，利用 WHERE stock >= ? 防超卖）
        var itemQw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.pzhu.mall.modules.order.entity.OrderItem>();
        itemQw.eq(com.pzhu.mall.modules.order.entity.OrderItem::getOrderId, orderId);
        List<com.pzhu.mall.modules.order.entity.OrderItem> items = orderItemMapper.selectList(itemQw);
        for (com.pzhu.mall.modules.order.entity.OrderItem item : items) {
            if (item.getSkuId() != null) {
                boolean ok = skuMapper.deductStock(item.getSkuId(), item.getQuantity());
                if (!ok) {
                    throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH, "商品库存不足");
                }
                // O-04 修复：同步更新 Product.stock 使用原子 UPDATE（GREATEST 防负），
                // 替代原"selectById + setStock + updateById"读改写——并发支付同一商品不同 SKU 时
                // 读改写会丢失更新导致商品总库存漂移。
                productMapper.deductStockUnchecked(item.getProductId(), item.getQuantity());
            } else {
                // H-4 修复：单规格商品走商品级原子扣减（UPDATE ... WHERE stock >= ?），
                // 原实现此分支无任何数据库扣减，Redis 预扣减丢失后即可无限超卖
                boolean ok = productMapper.deductStock(item.getProductId(), item.getQuantity());
                if (!ok) {
                    throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH, "商品库存不足");
                }
            }
        }

        // 积分正记录（按实付金额 1:1）
        pointsService.settleEarn(orderId, order.getUserId(), order.getPayAmount());

        // O-05 修复：支付记录落库（payment 表，任务书 7.3"支付记录管理"）。
        // 支付流水号复用订单号（订单号全局唯一且与支付一一对应），状态=1 成功。
        com.pzhu.mall.modules.order.entity.Payment payment = new com.pzhu.mall.modules.order.entity.Payment();
        payment.setOrderId(orderId);
        payment.setPayNo(order.getOrderNo());
        payment.setAmount(order.getPayAmount());
        payment.setPayType(payType);
        payment.setStatus(1);
        payment.setCallbackTime(LocalDateTime.now());
        payment.setCreateTime(LocalDateTime.now());
        paymentMapper.insert(payment);
        // 说明：购买行为（behaviorType=3）已在 OrderGroupProcessor.processGroup 下单时记录一次，
        // 此处不再重复记录（避免同一订单双倍购买行为污染推荐矩阵；赠品行也已在 processGroup 排除）。
    }

    /**
     * 评价订单项。
     */
    public void review(Long orderItemId, Integer rating, String content) {
        Long userId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();
        OrderItem item = orderItemMapper.selectById(orderItemId);
        if (item == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        // M-01 修复：赠品行（is_gift=1）不可单独评价（docs/13：前端不展示入口，后端兜底拦截）
        if (item.getIsGift() != null && item.getIsGift() == 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "赠品不支持单独评价");
        }

        // 写入 review 表（幂等：同一订单项只能评价一次）
        reviewService.submit(orderItemId, userId, rating, content, null);

        // 记录评价行为（behaviorType=4，仅好评计入，rating>=4）
        if (rating != null && rating >= 4) {
            behaviorService.record(userId, item.getProductId(), 4);
        }
    }

    private OrderVO toVO(Order order) {
        OrderVO vo = new OrderVO();
        vo.setOrderId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setShopId(order.getShopId());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setFreightAmount(order.getFreightAmount());
        vo.setPromotionDiscountAmount(order.getPromotionDiscountAmount());
        vo.setCouponDiscountAmount(order.getCouponDiscountAmount());
        vo.setPointsDeductAmount(order.getPointsDeductAmount());
        vo.setDiscountAmount(order.getDiscountAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setStatus(order.getStatus());
        vo.setStatusText(STATUS_MAP.getOrDefault(order.getStatus(), "未知"));
        vo.setAddressSnapshot(order.getAddressSnapshot());
        vo.setPayType(order.getPayType());
        vo.setPayTime(order.getPayTime());
        vo.setRemark(order.getRemark());
        vo.setCreateTime(order.getCreateTime());

        // Load order items
        var itemQw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.pzhu.mall.modules.order.entity.OrderItem>();
        itemQw.eq(com.pzhu.mall.modules.order.entity.OrderItem::getOrderId, order.getId());
        List<com.pzhu.mall.modules.order.entity.OrderItem> items = orderItemMapper.selectList(itemQw);
        if (items != null) {
            vo.setItems(items.stream().map(item -> {
                OrderItemVO iv = new OrderItemVO();
                iv.setId(item.getId());
                iv.setProductId(item.getProductId());
                iv.setSkuId(item.getSkuId());
                iv.setProductName(item.getProductNameSnapshot());
                iv.setProductImage(item.getProductImageSnapshot());
                iv.setPrice(item.getPrice());
                iv.setQuantity(item.getQuantity());
                iv.setIsGift(item.getIsGift());
                return iv;
            }).collect(java.util.stream.Collectors.toList()));
        }

        return vo;
    }

    /**
     * 批量转换，避免 N+1 查询。
     * 先一次性查出所有 orderId 对应的 order items，再分组构建 VO。
     */
    private List<OrderVO> toVOList(List<Order> orders) {
        if (orders.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        java.util.Set<Long> orderIds = orders.stream()
            .map(com.pzhu.mall.modules.order.entity.Order::getId)
            .collect(java.util.stream.Collectors.toSet());

        // 一次性查出所有 items，按 orderId 分组
        var itemQw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.pzhu.mall.modules.order.entity.OrderItem>();
        itemQw.in(com.pzhu.mall.modules.order.entity.OrderItem::getOrderId, orderIds);
        List<com.pzhu.mall.modules.order.entity.OrderItem> allItems = orderItemMapper.selectList(itemQw);

        java.util.Map<Long, List<com.pzhu.mall.modules.order.entity.OrderItem>> itemsByOrder = allItems.stream()
            .collect(java.util.stream.Collectors.groupingBy(com.pzhu.mall.modules.order.entity.OrderItem::getOrderId));

        return orders.stream().map(order -> {
            OrderVO vo = new OrderVO();
            vo.setOrderId(order.getId());
            vo.setOrderNo(order.getOrderNo());
            vo.setShopId(order.getShopId());
            vo.setTotalAmount(order.getTotalAmount());
            vo.setFreightAmount(order.getFreightAmount());
            vo.setPromotionDiscountAmount(order.getPromotionDiscountAmount());
            vo.setCouponDiscountAmount(order.getCouponDiscountAmount());
            vo.setPointsDeductAmount(order.getPointsDeductAmount());
            vo.setDiscountAmount(order.getDiscountAmount());
            vo.setPayAmount(order.getPayAmount());
            vo.setStatus(order.getStatus());
            vo.setStatusText(STATUS_MAP.getOrDefault(order.getStatus(), "未知"));
            vo.setAddressSnapshot(order.getAddressSnapshot());
            vo.setPayType(order.getPayType());
            vo.setPayTime(order.getPayTime());
            vo.setRemark(order.getRemark());
            vo.setCreateTime(order.getCreateTime());

            List<com.pzhu.mall.modules.order.entity.OrderItem> items = itemsByOrder.getOrDefault(order.getId(), java.util.Collections.emptyList());
            vo.setItems(items.stream().map(item -> {
                OrderItemVO iv = new OrderItemVO();
                iv.setId(item.getId());
                iv.setProductId(item.getProductId());
                iv.setSkuId(item.getSkuId());
                iv.setProductName(item.getProductNameSnapshot());
                iv.setProductImage(item.getProductImageSnapshot());
                iv.setPrice(item.getPrice());
                iv.setQuantity(item.getQuantity());
                iv.setIsGift(item.getIsGift());
                return iv;
            }).collect(java.util.stream.Collectors.toList()));

            return vo;
        }).collect(java.util.stream.Collectors.toList());
    }
}
