package com.pzhu.mall.modules.order.service;

import com.pzhu.mall.modules.behavior.service.BehaviorService;
import com.pzhu.mall.modules.marketing.service.CouponService;
import com.pzhu.mall.modules.marketing.service.PointsService;
import com.pzhu.mall.modules.order.dto.CreateOrderDTO;
import com.pzhu.mall.modules.order.dto.ProductItemDTO;
import com.pzhu.mall.modules.order.entity.Order;
import com.pzhu.mall.modules.order.entity.OrderItem;
import com.pzhu.mall.modules.order.mapper.OrderMapper;
import com.pzhu.mall.modules.order.mapper.OrderItemMapper;
import com.pzhu.mall.modules.order.component.OrderNoGenerator;
import com.pzhu.mall.modules.order.vo.OrderVO;
import com.pzhu.mall.modules.product.entity.Product;
import com.pzhu.mall.modules.product.entity.Sku;
import com.pzhu.mall.modules.product.mapper.ProductMapper;
import com.pzhu.mall.modules.product.mapper.SkuMapper;
import com.pzhu.mall.modules.logistics.service.FreightService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单分组处理器。
 * <p>独立的 {@code @Service} 类，确保 {@link Transactional} 注解被 Spring AOP 代理正确拦截。
 * 每个店铺分组在此类中独立开启事务，单组失败不影响其他组。</p>
 */
@Service
public class OrderGroupProcessor {

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private OrderItemMapper orderItemMapper;

    @Resource
    private OrderNoGenerator orderNoGenerator;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private SkuMapper skuMapper;

    @Resource
    private FreightService freightService;

    @Resource
    private com.pzhu.mall.modules.marketing.service.PromotionService promotionService;

    @Resource
    private PointsService pointsService;

    @Resource
    private CouponService couponService;

    @Resource
    private BehaviorService behaviorService;

    /**
     * 处理单个店铺分组（独立事务）。
     * <p>
     * 内部保证：创建 Order → 批量插入 OrderItem → 积分抵扣结算 → 标记优惠券已使用，
     * 任一环节失败时整个分组回滚，调用方负责 Redis 库存归还。
     *
     * @param applyPoints 是否应用积分抵扣（仅第一个成功分组传 true）
     */
    @Transactional(rollbackFor = Exception.class)
    public List<OrderVO> processGroup(Long userId, Long shopId,
                                      List<ProductItemDTO> groupItems,
                                      String addressSnapshot,
                                      String province,
                                      CreateOrderDTO dto,
                                      boolean applyPoints) {
        List<OrderVO> result = new ArrayList<>();

        // 计算商品金额
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

        // 计算运费
        BigDecimal freightAmount = freightService.calculate(shopId, province, goodsAmount);

        // 促销优惠
        BigDecimal promotionDiscount = promotionService.calculateDiscount(shopId, goodsAmount);

        // 优惠券抵扣
        BigDecimal couponDiscount = BigDecimal.ZERO;
        if (dto.getCouponId() != null) {
            couponDiscount = couponService.calculateDiscount(dto.getCouponId(), goodsAmount);
        }

        // 积分抵扣
        BigDecimal pointsDeduct = BigDecimal.ZERO;
        Integer pointsUsed = 0;
        if (applyPoints && Boolean.TRUE.equals(dto.getUsePoints())) {
            BigDecimal[] deductResult = pointsService.calculateDeduct(userId, goodsAmount);
            pointsDeduct = deductResult[0];
            pointsUsed = deductResult[1].intValue();
        }

        // 实付金额
        BigDecimal payAmount = goodsAmount.add(freightAmount)
                .subtract(promotionDiscount)
                .subtract(couponDiscount)
                .subtract(pointsDeduct);

        // 创建订单
        Order order = new Order();
        order.setOrderNo(orderNoGenerator.generate());
        order.setUserId(userId);
        order.setShopId(shopId);
        order.setTotalAmount(goodsAmount.add(freightAmount));
        order.setFreightAmount(freightAmount);
        order.setPromotionDiscountAmount(promotionDiscount);
        order.setCouponDiscountAmount(couponDiscount);
        order.setPointsDeductAmount(pointsDeduct);
        order.setDiscountAmount(promotionDiscount.add(couponDiscount).add(pointsDeduct));
        order.setPayAmount(payAmount);
        order.setStatus(0); // 待付款
        order.setAddressSnapshot(addressSnapshot);
        order.setRemark(dto.getRemark());
        order.setIsDeleted(0);
        orderMapper.insert(order);

        // 批量插入订单明细
        for (OrderItem oi : orderItems) {
            oi.setOrderId(order.getId());
            orderItemMapper.insert(oi);
        }

        // 积分抵扣记录
        if (pointsUsed > 0) {
            pointsService.settleDeduct(userId, pointsUsed, order.getId());
        }

        // 标记优惠券已使用
        if (dto.getCouponId() != null) {
            couponService.markUsed(dto.getCouponId(), order.getId());
        }

        // 记录购买行为
        for (OrderItem oi : orderItems) {
            behaviorService.record(userId, oi.getProductId(), 3);
        }

        result.add(buildOrderVO(order));
        return result;
    }

    /**
     * 构建订单 VO（含订单明细加载）。
     */
    private OrderVO buildOrderVO(Order order) {
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
        vo.setAddressSnapshot(order.getAddressSnapshot());
        vo.setPayType(order.getPayType());
        vo.setPayTime(order.getPayTime());
        vo.setRemark(order.getRemark());
        vo.setCreateTime(order.getCreateTime());

        // 加载订单明细
        var itemQw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.pzhu.mall.modules.order.entity.OrderItem>();
        itemQw.eq(com.pzhu.mall.modules.order.entity.OrderItem::getOrderId, order.getId());
        List<com.pzhu.mall.modules.order.entity.OrderItem> items = orderItemMapper.selectList(itemQw);
        if (items != null) {
            vo.setItems(items.stream().map(item -> {
                com.pzhu.mall.modules.order.vo.OrderItemVO iv = new com.pzhu.mall.modules.order.vo.OrderItemVO();
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
}
