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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * 按店铺拆单后每组独立执行，每组通过独立的 {@code @Transactional} 方法处理，
     * 单组因库存不足等原因失败时，已成功创建的子订单不回滚（符合设计文档要求）。
     * 每组内部通过 Redis 预扣减 + 失败手动回滚保证一致性。
     */
    @Transactional(rollbackFor = Exception.class)
    public List<OrderVO> createOrder(CreateOrderDTO dto) {
        Long userId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();

        // 0. 幂等校验（Redis SET NX）
        String requestId = dto.getRequestId();
        if (requestId == null || requestId.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "requestId 不能为空");
        }
        String idempotentKey = RedisKeyPrefix.ORDER + ":idempotent:" + requestId;
        Boolean alreadySet = stringRedisTemplate.opsForValue().setIfAbsent(idempotentKey, "1", IDEMPOTENT_TTL_HOURS, java.util.concurrent.TimeUnit.HOURS);
        if (alreadySet != null && !alreadySet) {
            // 重复请求：根据设计文档 §1.5，直接返回已创建的订单列表
            // 此处简化处理：返回空列表（生产环境应缓存并返回原始订单列表）
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单已提交，请勿重复操作");
        }

        // 1. 获取商品列表
        List<ProductItemDTO> items = dto.getProductItems();
        if (items == null || items.isEmpty()) {
            // 从购物车读取
            List<Cart> carts = cartMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Cart>()
                    .in(Cart::getId, dto.getCartItemIds())
            );
            items = carts.stream().map(c -> {
                ProductItemDTO p = new ProductItemDTO();
                p.setProductId(c.getProductId());
                p.setSkuId(c.getSkuId());
                p.setQuantity(c.getQuantity());
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
            byShop.computeIfAbsent(product.getShopId(), k -> new ArrayList<>()).add(item);
        }

        // 3. 获取收货地址
        Address address = addressMapper.selectById(dto.getAddressId());
        if (address == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        String addressSnapshot = String.format("{\"receiver\":\"%s\",\"phone\":\"%s\",\"province\":\"%s\",\"city\":\"%s\",\"district\":\"%s\",\"detail\":\"%s\"}",
                address.getReceiver(), address.getPhone(), address.getProvince(), address.getCity(), address.getDistrict(), address.getDetail());

        List<OrderVO> allOrders = new ArrayList<>();
        boolean pointsProcessed = false;
        boolean cartDeleted = false;
        List<String> failedShops = new ArrayList<>();

        // 4. 按分组创建订单（每组独立事务，单组失败跳过并记录，其余组继续）
        for (Map.Entry<Long, List<ProductItemDTO>> entry : byShop.entrySet()) {
            Long shopId = entry.getKey();
            List<ProductItemDTO> groupItems = entry.getValue();

            List<Long> deductedSkus = new ArrayList<>();
            List<Integer> deductedQtys = new ArrayList<>();
            boolean stockFailed = false;
            try {
                // 4.7 库存预扣减（该分组内全部 SKU，失败则回滚该分组已扣减的库存）
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
                    }
                }
            } catch (Exception e) {
                stockFailed = true;
            }
            if (stockFailed) {
                for (int i = 0; i < deductedSkus.size(); i++) {
                    stockService.rollback(deductedSkus.get(i), deductedQtys.get(i));
                }
                failedShops.add("店铺" + shopId + "库存不足");
                continue;
            }

            // 4.8 调用独立事务方法处理该分组（insert order + orderItems + 积分/优惠券 + 购物车清理）
            boolean applyPoints = !pointsProcessed && Boolean.TRUE.equals(dto.getUsePoints());
            List<OrderVO> groupOrders;
            try {
                groupOrders = orderGroupProcessor.processGroup(userId, shopId, groupItems, addressSnapshot, address.getProvince(), dto, applyPoints);
            } catch (Exception e) {
                // 事务回滚后，归还 Redis 预扣减的库存
                for (int i = 0; i < deductedSkus.size(); i++) {
                    stockService.rollback(deductedSkus.get(i), deductedQtys.get(i));
                }
                failedShops.add("店铺" + shopId + "下单失败");
                continue;
            }
            allOrders.addAll(groupOrders);

            // 标记积分已处理（仅第一个成功分组处理一次）
            if (applyPoints) {
                pointsProcessed = true;
            }
            // 删除已提交的购物车项（仅处理一次）
            if (dto.getCartItemIds() != null && !cartDeleted) {
                cartMapper.deleteBatchIds(dto.getCartItemIds());
                cartDeleted = true;
            }
        }

        // 汇总部分失败的提示
        if (!failedShops.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, String.join("；", failedShops) + "，其余订单已创建");
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
     */
    public List<OrderVO> listByUser() {
        Long userId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();
        List<Order> orders = orderMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreateTime)
        );
        return toVOList(orders);
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
     * 商家端：发货。
     */
    @Transactional
    public void ship(Long orderId, String logisticsCompany, String trackingNo) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != 1) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        order.setStatus(2); // 已发货
        orderMapper.updateById(order);

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
     * 取消订单。
     */
    @Transactional
    public void cancelOrder(Long orderId) {
        Long userId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != 0) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        order.setStatus(5); // 已取消
        orderMapper.updateById(order);

        // 库存归还
        var itemQw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.pzhu.mall.modules.order.entity.OrderItem>();
        itemQw.eq(com.pzhu.mall.modules.order.entity.OrderItem::getOrderId, orderId);
        List<com.pzhu.mall.modules.order.entity.OrderItem> items = orderItemMapper.selectList(itemQw);
        for (com.pzhu.mall.modules.order.entity.OrderItem item : items) {
            if (item.getSkuId() != null) {
                stockService.rollback(item.getSkuId(), item.getQuantity());
            }
        }

        // 释放优惠券（检查 couponDiscountAmount 而非 discountAmount，后者包含促销折扣和积分抵扣）
        if (order.getCouponDiscountAmount() != null && order.getCouponDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            couponService.releaseByOrderId(orderId);
        }

        // 扣回积分（若订单使用了积分抵扣）
        if (order.getPointsDeductAmount() != null && order.getPointsDeductAmount().compareTo(BigDecimal.ZERO) > 0) {
            pointsService.clawback(orderId);
        }
    }

    /**
     * 确认收货。
     */
    @Transactional
    public void confirmReceive(Long orderId) {
        Long userId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        order.setStatus(4); // 已完成
        orderMapper.updateById(order);
    }

    /**
     * 支付订单（模拟）。
     */
    @Transactional
    public void pay(Long orderId, Integer payType) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != 0) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_PAID);
        }
        // 幂等保护：通过 Redis 标记已支付
        String payKey = RedisKeyPrefix.ORDER + ":paid:" + orderId;
        Boolean paid = stringRedisTemplate.opsForValue().setIfAbsent(payKey, "1", 30, java.util.concurrent.TimeUnit.DAYS);
        if (paid == null || !paid) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_PAID);
        }
        order.setStatus(1); // 待发货
        order.setPayType(payType);
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);

        // 真实库存扣减（数据库原子操作，利用 WHERE stock >= ? 防超卖）
        var itemQw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.pzhu.mall.modules.order.entity.OrderItem>();
        itemQw.eq(com.pzhu.mall.modules.order.entity.OrderItem::getOrderId, orderId);
        List<com.pzhu.mall.modules.order.entity.OrderItem> items = orderItemMapper.selectList(itemQw);
        for (com.pzhu.mall.modules.order.entity.OrderItem item : items) {
            if (item.getSkuId() != null) {
                skuMapper.deductStock(item.getSkuId(), item.getQuantity());
            }
        }

        // 积分正记录（按实付金额 1:1）
        pointsService.settleEarn(orderId, order.getUserId(), order.getPayAmount());

        // 记录购买行为（behaviorType=3）
        List<com.pzhu.mall.modules.order.entity.OrderItem> payItems = orderItemMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.pzhu.mall.modules.order.entity.OrderItem>()
                .eq(com.pzhu.mall.modules.order.entity.OrderItem::getOrderId, orderId)
        );
        for (com.pzhu.mall.modules.order.entity.OrderItem item : payItems) {
            behaviorService.record(order.getUserId(), item.getProductId(), 3);
        }
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
