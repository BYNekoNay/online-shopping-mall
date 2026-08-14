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
import com.pzhu.mall.modules.order.component.StockService;
import com.pzhu.mall.modules.order.vo.OrderVO;
import com.pzhu.mall.modules.product.entity.Product;
import com.pzhu.mall.modules.product.entity.Sku;
import com.pzhu.mall.modules.product.mapper.ProductMapper;
import com.pzhu.mall.modules.product.mapper.SkuMapper;
import com.pzhu.mall.modules.cart.mapper.CartMapper;
import com.pzhu.mall.modules.logistics.service.FreightService;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.common.enums.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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

    private static final Logger log = LoggerFactory.getLogger(OrderGroupProcessor.class);

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
    private StockService stockService;

    @Resource
    private com.pzhu.mall.modules.marketing.service.PromotionService promotionService;

    @Resource
    private PointsService pointsService;

    @Resource
    private CouponService couponService;

    @Resource
    private BehaviorService behaviorService;

    @Resource
    private CartMapper cartMapper;

    /**
     * 处理单个店铺分组（独立事务）。
     * <p>
     * 内部保证：创建 Order → 批量插入 OrderItem → 积分抵扣结算 → 标记优惠券已使用 → 清理购物车，
     * 任一环节失败时整个分组回滚，调用方负责 Redis 库存归还。
     *
     * @param applyPoints 是否应用积分抵扣（仅第一个成功分组传 true）
     * @param applyCoupon 是否应用优惠券抵扣并核销（仅第一个成功分组传 true，H-6 修复）
     * @param deleteCartItemIds 需要在事务内删除的购物车项 ID（仅第一个成功分组传入）
     */
    @Transactional(rollbackFor = Exception.class)
    public List<OrderVO> processGroup(Long userId, Long shopId,
                                      List<ProductItemDTO> groupItems,
                                      String addressSnapshot,
                                      String province,
                                      CreateOrderDTO dto,
                                      boolean applyPoints,
                                      boolean applyCoupon,
                                      List<Long> deleteCartItemIds) {
        List<OrderVO> result = new ArrayList<>();

        // 计算商品金额
        BigDecimal goodsAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();
        for (ProductItemDTO item : groupItems) {
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "商品数量必须大于0");
            }
            Product product = productMapper.selectById(item.getProductId());
            Sku sku = item.getSkuId() != null ? skuMapper.selectById(item.getSkuId()) : null;
            // C-1 修复：计价前校验 SKU 与商品的绑定关系（纵深防御，此处是单价最终决定点）。
            // skuId 非空但 SKU 不存在或归属其他商品时显式抛错，禁止静默回退商品价
            if (item.getSkuId() != null) {
                if (sku == null) {
                    throw new BusinessException(ErrorCode.SKU_NOT_FOUND);
                }
                if (!item.getProductId().equals(sku.getProductId())) {
                    throw new BusinessException(ErrorCode.SKU_PRODUCT_MISMATCH);
                }
            }
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

        // M-01 修复：满赠促销（type=3）——商品金额达标时赠送指定 SKU（赠品行 price=0、is_gift=1）。
        // 赠品需预扣库存，库存不足/配置非法/异常时静默跳过赠送、不阻断下单（docs/10 §2.4.2）。
        // 赠品行不参与 goodsAmount/freight/payAmount 计算；随订单一并出库（支付时 DB 实扣），
        // 事务回滚时通过 afterCompletion 自动归还预扣的 Redis 库存。
        com.pzhu.mall.modules.marketing.service.PromotionService.GiftInfo gift =
                promotionService.matchGift(shopId, goodsAmount);
        if (gift != null) {
            try {
                Sku giftSku = skuMapper.selectById(gift.giftSkuId());
                Product giftProduct = productMapper.selectById(gift.giftProductId());
                boolean valid = giftSku != null && giftProduct != null
                        && giftProduct.getIsDeleted() != null && giftProduct.getIsDeleted() == 0
                        && gift.giftProductId().equals(giftSku.getProductId());
                if (!valid) {
                    log.warn("[订单] 满赠：赠品 SKU 绑定校验失败，跳过赠送 giftProductId={} giftSkuId={}",
                            gift.giftProductId(), gift.giftSkuId());
                } else if (stockService.deduct(gift.giftSkuId(), gift.giftQuantity())) {
                    OrderItem giftItem = new OrderItem();
                    giftItem.setProductId(gift.giftProductId());
                    giftItem.setSkuId(gift.giftSkuId());
                    giftItem.setProductNameSnapshot(giftProduct.getName());
                    giftItem.setProductImageSnapshot(giftSku.getImage() != null ? giftSku.getImage() : giftProduct.getMainImage());
                    giftItem.setPrice(BigDecimal.ZERO);
                    giftItem.setQuantity(gift.giftQuantity());
                    giftItem.setIsGift(1);
                    orderItems.add(giftItem);
                    // 事务回滚时自动归还赠品预扣的 Redis 库存（提交成功则随订单出库）
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCompletion(int status) {
                            if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                                try {
                                    stockService.rollback(gift.giftSkuId(), gift.giftQuantity());
                                } catch (Exception e) {
                                    log.error("[订单] 回滚后赠品库存归还失败 skuId={}（需人工对账）", gift.giftSkuId(), e);
                                }
                            }
                        }
                    });
                    log.info("[订单] 满赠：店铺{} 金额{} 赠送 SKU{} ×{}", shopId, goodsAmount, gift.giftSkuId(), gift.giftQuantity());
                } else {
                    log.warn("[订单] 满赠：赠品库存不足，跳过赠送 shopId={} giftSkuId={}", shopId, gift.giftSkuId());
                }
            } catch (Exception e) {
                log.warn("[订单] 满赠处理异常，跳过赠送（不阻断下单）shopId={}", shopId, e);
            }
        }

        // 计算运费
        BigDecimal freightAmount = freightService.calculate(shopId, province, goodsAmount);

        // 促销优惠
        BigDecimal promotionDiscount = promotionService.calculateDiscount(shopId, goodsAmount);

        // 优惠券抵扣（通过 userCouponId 查找关联的 Coupon 模板）
        // H-6 修复：仅在首个成功分组计价，防止一张券在多个店铺分组被重复抵扣
        // M-02 修复：传 userId/shopId/品类ID 列表，校验券适用范围（店铺券/品类券），不匹配返回 0
        BigDecimal couponDiscount = BigDecimal.ZERO;
        if (applyCoupon && dto.getUserCouponId() != null) {
            List<Long> categoryIds = new ArrayList<>();
            for (ProductItemDTO item : groupItems) {
                Product p = productMapper.selectById(item.getProductId());
                if (p != null && p.getCategoryId() != null) {
                    categoryIds.add(p.getCategoryId());
                }
            }
            couponDiscount = couponService.calculateDiscountByUserCoupon(
                    dto.getUserCouponId(), goodsAmount, userId, shopId, categoryIds);
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
        if (payAmount.compareTo(BigDecimal.ZERO) < 0) {
            payAmount = BigDecimal.ZERO;
        }

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
        // H-6 修复：仅在首个成功分组核销一次；原实现每个分组都 markUsed，
        // 第二分组因券已被首组事务置为已使用（WHERE status=0 命中 0 行）必然失败
        if (applyCoupon && dto.getUserCouponId() != null) {
            couponService.markUsed(dto.getUserCouponId(), order.getId(), userId);
        }

        // 记录购买行为
        // M-06 修复：行为埋点为 best-effort，失败不应回滚订单事务
        // M-01 修复：排除赠品行（is_gift=1），避免赠品商品被记为"购买"污染推荐矩阵
        try {
            for (OrderItem oi : orderItems) {
                if (oi.getIsGift() != null && oi.getIsGift() == 1) {
                    continue;
                }
                behaviorService.record(userId, oi.getProductId(), 3);
            }
        } catch (Exception e) {
            log.warn("[订单] 购买行为埋点失败（不影响订单）orderId={}", order.getId(), e);
        }

        // 在事务内删除已提交的购物车项
        // M-07 修复：购物车清理为 best-effort，失败不应回滚订单事务
        // M-8 修复：deleteCartItemIds 为本成功分组来源的购物车项（按分组精确传入）
        if (deleteCartItemIds != null && !deleteCartItemIds.isEmpty()) {
            try {
                // H-3 修复：附加 user_id 条件，防止越权删除他人购物车项（IDOR）
                cartMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.pzhu.mall.modules.cart.entity.Cart>()
                        .in(com.pzhu.mall.modules.cart.entity.Cart::getId, deleteCartItemIds)
                        .eq(com.pzhu.mall.modules.cart.entity.Cart::getUserId, userId));
            } catch (Exception e) {
                log.warn("[订单] 购物车清理失败（不影响订单）orderId={} cartItemIds={}", order.getId(), deleteCartItemIds, e);
            }
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
