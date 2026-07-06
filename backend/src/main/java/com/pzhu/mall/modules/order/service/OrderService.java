package com.pzhu.mall.modules.order.service;

import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.modules.cart.entity.Cart;
import com.pzhu.mall.modules.cart.mapper.CartMapper;
import com.pzhu.mall.modules.marketing.service.CouponService;
import com.pzhu.mall.modules.order.dto.CreateOrderDTO;
import com.pzhu.mall.modules.order.dto.ProductItemDTO;
import com.pzhu.mall.modules.order.entity.Order;
import com.pzhu.mall.modules.order.entity.OrderItem;
import com.pzhu.mall.modules.order.mapper.OrderMapper;
import com.pzhu.mall.modules.order.mapper.OrderItemMapper;
import com.pzhu.mall.modules.order.vo.OrderVO;
import com.pzhu.mall.modules.product.entity.Product;
import com.pzhu.mall.modules.product.entity.Sku;
import com.pzhu.mall.modules.product.mapper.ProductMapper;
import com.pzhu.mall.modules.product.mapper.SkuMapper;
import com.pzhu.mall.modules.user.entity.Address;
import com.pzhu.mall.modules.user.mapper.AddressMapper;
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

    private static final String ORDER_STATUS_MAP = "{\"0\":\"待付款\",\"1\":\"待发货\",\"2\":\"已发货\",\"3\":\"已收货\",\"4\":\"已完成\",\"5\":\"已取消\",\"6\":\"退款中\",\"7\":\"已退款\"}";

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
    private AddressMapper addressMapper;

    @Resource
    private CouponService couponService;

    @Resource
    private com.pzhu.mall.modules.marketing.service.PromotionService promotionService;

    @Resource
    private com.pzhu.mall.modules.marketing.service.PointsService pointsService;

    @Resource
    private com.pzhu.mall.modules.logistics.service.FreightService freightService;

    /**
     * 提交订单（核心链路）。
     */
    @Transactional
    public List<OrderVO> createOrder(CreateOrderDTO dto) {
        Long userId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();

        // 幂等校验
        // TODO: Redis requestId 幂等缓存（待 Redis 集成完成后实现）

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
            if (product.getStatus() != 1) {
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
        boolean firstGroup = true;

        // 4. 按分组创建订单
        for (Map.Entry<Long, List<ProductItemDTO>> entry : byShop.entrySet()) {
            Long shopId = entry.getKey();
            List<ProductItemDTO> groupItems = entry.getValue();

            // 4.1 计算商品金额（促销折扣后）
            BigDecimal goodsAmount = BigDecimal.ZERO;
            List<OrderItem> orderItems = new ArrayList<>();
            for (ProductItemDTO item : groupItems) {
                Product product = productMapper.selectById(item.getProductId());
                Sku sku = item.getSkuId() != null ? skuMapper.selectById(item.getSkuId()) : null;
                BigDecimal unitPrice = sku != null ? sku.getPrice() : product.getPrice();
                BigDecimal itemAmount = unitPrice.multiply(new BigDecimal(item.getQuantity()));
                goodsAmount = goodsAmount.add(itemAmount);

                OrderItem oi = new OrderItem();
                oi.setProductId(item.getProductId());
                oi.setSkuId(item.getSkuId());
                oi.setProductNameSnapshot(product.getName());
                oi.setProductImageSnapshot(sku != null ? sku.getImage() : product.getMainImage());
                oi.setPrice(unitPrice);
                oi.setQuantity(item.getQuantity());
                oi.setIsGift(0);
                orderItems.add(oi);
            }

            // 4.2 计算运费
            BigDecimal freightAmount = freightService.calculate(shopId, address.getProvince(), goodsAmount);

            // 4.3 促销优惠
            BigDecimal promotionDiscount = promotionService.calculateDiscount(shopId, goodsAmount);

            // 4.4 优惠券抵扣
            BigDecimal couponDiscount = BigDecimal.ZERO;
            if (dto.getCouponId() != null) {
                couponDiscount = couponService.calculateDiscount(dto.getCouponId(), goodsAmount);
            }

            // 4.5 积分抵扣（仅第一组）
            BigDecimal pointsDeduct = BigDecimal.ZERO;
            Integer pointsUsed = 0;
            if (firstGroup && Boolean.TRUE.equals(dto.getUsePoints())) {
                java.math.BigDecimal[] result = pointsService.calculateDeduct(userId, goodsAmount);
                pointsDeduct = result[0];
                pointsUsed = result[1].intValue();
            }
            firstGroup = false;

            // 4.6 实付金额
            BigDecimal payAmount = goodsAmount.add(freightAmount)
                    .subtract(promotionDiscount)
                    .subtract(couponDiscount)
                    .subtract(pointsDeduct);

            // 4.7 创建订单
            Order order = new Order();
            order.setOrderNo(generateOrderNo());
            order.setUserId(userId);
            order.setShopId(shopId);
            order.setTotalAmount(goodsAmount.add(freightAmount));
            order.setFreightAmount(freightAmount);
            order.setDiscountAmount(promotionDiscount.add(couponDiscount).add(pointsDeduct));
            order.setPayAmount(payAmount);
            order.setStatus(0); // 待付款
            order.setAddressSnapshot(addressSnapshot);
            order.setRemark(dto.getRemark());
            order.setIsDeleted(0);
            orderMapper.insert(order);

            // 4.8 批量插入订单明细
            for (OrderItem oi : orderItems) {
                oi.setOrderId(order.getId());
                orderItemMapper.insert(oi);
            }

            // 4.9 积分抵扣记录
            if (pointsUsed > 0) {
                pointsService.settleDeduct(userId, pointsUsed, order.getId());
            }

            // 4.10 标记优惠券已使用
            if (dto.getCouponId() != null) {
                couponService.markUsed(dto.getCouponId(), order.getId());
            }

            // 4.11 删除已提交的购物车项
            if (dto.getCartItemIds() != null) {
                cartMapper.deleteBatchIds(dto.getCartItemIds());
            }

            allOrders.add(toVO(order));
        }

        return allOrders;
    }

    /**
     * 生成订单号。
     */
    private String generateOrderNo() {
        String timestamp = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        String random = String.format("%06d", new Random().nextInt(1000000));
        return timestamp + random;
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
        return orders.stream().map(this::toVO).collect(Collectors.toList());
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
        // TODO: 库存归还
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
        order.setStatus(1); // 待发货
        order.setPayType(payType);
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);

        // TODO: 真实库存扣减
        // TODO: 积分正记录
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
        // TODO: 写入 review 表
    }

    private OrderVO toVO(Order order) {
        OrderVO vo = new OrderVO();
        vo.setOrderId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setShopId(order.getShopId());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setFreightAmount(order.getFreightAmount());
        vo.setDiscountAmount(order.getDiscountAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setStatus(order.getStatus());
        vo.setAddressSnapshot(order.getAddressSnapshot());
        vo.setPayType(order.getPayType());
        vo.setPayTime(order.getPayTime());
        vo.setRemark(order.getRemark());
        vo.setCreateTime(order.getCreateTime());
        return vo;
    }
}
