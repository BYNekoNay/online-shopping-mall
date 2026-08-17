package com.pzhu.mall.modules.order.controller;

import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.modules.marketing.service.CouponService;
import com.pzhu.mall.modules.marketing.service.PointsService;
import com.pzhu.mall.modules.marketing.service.PromotionService;
import com.pzhu.mall.modules.order.dto.ProductItemDTO;
import com.pzhu.mall.modules.order.mapper.OrderItemMapper;
import com.pzhu.mall.modules.order.vo.OrderEstimateVO;
import com.pzhu.mall.common.enums.ProductStatus;
import com.pzhu.mall.modules.product.entity.Product;
import com.pzhu.mall.modules.product.entity.Sku;
import com.pzhu.mall.modules.product.mapper.ProductMapper;
import com.pzhu.mall.modules.product.mapper.SkuMapper;
import com.pzhu.mall.modules.user.entity.Address;
import com.pzhu.mall.modules.user.mapper.AddressMapper;
import com.pzhu.mall.modules.logistics.service.FreightService;
import com.pzhu.mall.security.LoginUserContext;
import com.pzhu.mall.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单估价控制器。
 */
@Tag(name = "订单估价")
@RestController
@RequestMapping("/api/orders/estimate")
@RequireRole(1)
public class OrderEstimateController {

    @Resource
    private OrderItemMapper orderItemMapper;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private SkuMapper skuMapper;

    @Resource
    private AddressMapper addressMapper;

    @Resource
    private com.pzhu.mall.modules.logistics.service.FreightService freightService;

    @Resource
    private com.pzhu.mall.modules.marketing.service.PromotionService promotionService;

    @Resource
    private com.pzhu.mall.modules.marketing.service.CouponService couponService;

    @Resource
    private com.pzhu.mall.modules.marketing.service.PointsService pointsService;

    @Resource
    private com.pzhu.mall.modules.shop.mapper.ShopMapper shopMapper;

    // FRONT-07 修复：估价与下单同口径——通过 UserCoupon 记录校验券归属/状态/适用范围
    @Resource
    private com.pzhu.mall.modules.marketing.mapper.UserCouponMapper userCouponMapper;

    @Operation(summary = "订单估价（按店铺分组）")
    @PostMapping
    public Result<List<OrderEstimateVO>> estimate(@RequestBody EstimateRequest request) {
        Long userId = LoginUserContext.getCurrentUserId();

        // 获取收货地址信息
        Address address = addressMapper.selectById(request.getAddressId());
        String province = address != null ? address.getProvince() : "";

        List<ProductItemDTO> items = request.getProductItems();
        if (items == null || items.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        // 按 shopId 分组
        java.util.Map<Long, List<ProductItemDTO>> byShop = new java.util.LinkedHashMap<>();
        for (ProductItemDTO item : items) {
            Product product = productMapper.selectById(item.getProductId());
            if (product == null || product.getIsDeleted() == 1) continue;
            if (ProductStatus.of(product.getStatus()) != ProductStatus.ONLINE) continue;
            byShop.computeIfAbsent(product.getShopId(), k -> new ArrayList<>()).add(item);
        }

        List<OrderEstimateVO> result = new ArrayList<>();
        // FRONT-07 修复：与 OrderService.createOrder/OrderGroupProcessor 同口径——
        // 优惠券、积分抵扣为单一资源，跨店铺分组仅应用一次（首个分组），
        // 避免估价在多个分组重复抵扣导致"确认页显示金额 < 实际下单实付"
        boolean pointsProcessed = false;
        boolean couponProcessed = false;
        for (java.util.Map.Entry<Long, List<ProductItemDTO>> entry : byShop.entrySet()) {
            Long shopId = entry.getKey();
            List<ProductItemDTO> groupItems = entry.getValue();

            BigDecimal goodsAmount = BigDecimal.ZERO;
            List<OrderEstimateVO.ItemEstimateVO> itemVOs = new ArrayList<>();
            // FRONT-07 修复：收集分组内商品品类 ID，供品类券适用范围校验（与下单侧一致）
            java.util.Set<Long> categoryIds = new java.util.LinkedHashSet<>();
            for (ProductItemDTO item : groupItems) {
                Product product = productMapper.selectById(item.getProductId());
                if (product != null && product.getCategoryId() != null) {
                    categoryIds.add(product.getCategoryId());
                }
                Sku sku = item.getSkuId() != null ? skuMapper.selectById(item.getSkuId()) : null;
                // C-1 修复：估价与下单同口径——校验 SKU 与商品的绑定关系，
                // 防止伪造"商品A+商品B的SKU"组合得到被篡改的估价金额
                if (item.getSkuId() != null) {
                    if (sku == null) {
                        throw new com.pzhu.mall.common.exception.BusinessException(
                                com.pzhu.mall.common.enums.ErrorCode.SKU_NOT_FOUND);
                    }
                    if (!item.getProductId().equals(sku.getProductId())) {
                        throw new com.pzhu.mall.common.exception.BusinessException(
                                com.pzhu.mall.common.enums.ErrorCode.SKU_PRODUCT_MISMATCH);
                    }
                }
                BigDecimal unitPrice = sku != null ? sku.getPrice() : product.getPrice();
                BigDecimal itemAmount = unitPrice.multiply(new BigDecimal(item.getQuantity()));
                goodsAmount = goodsAmount.add(itemAmount);

                OrderEstimateVO.ItemEstimateVO itemVO = new OrderEstimateVO.ItemEstimateVO();
                itemVO.setProductId(item.getProductId());
                itemVO.setProductName(product.getName());
                itemVO.setProductImage(sku != null ? sku.getImage() : product.getMainImage());
                itemVO.setPrice(unitPrice);
                itemVO.setQuantity(item.getQuantity());
                itemVOs.add(itemVO);
            }

            BigDecimal freightAmount = freightService.calculate(shopId, province, goodsAmount);
            BigDecimal promotionDiscount = promotionService.calculateDiscount(shopId, goodsAmount);

            // 优惠券抵扣（FRONT-07 修复：仅首个分组应用一次，并通过 UserCoupon 记录校验
            // 归属/状态/有效期/店铺券/品类券适用范围，与下单侧 OrderGroupProcessor 完全同口径）
            BigDecimal couponDiscount = BigDecimal.ZERO;
            if (request.getCouponId() != null && !couponProcessed) {
                couponProcessed = true;
                com.pzhu.mall.modules.marketing.entity.UserCoupon userCoupon =
                        userCouponMapper.selectOne(
                                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.pzhu.mall.modules.marketing.entity.UserCoupon>()
                                        .eq(com.pzhu.mall.modules.marketing.entity.UserCoupon::getUserId, userId)
                                        .eq(com.pzhu.mall.modules.marketing.entity.UserCoupon::getCouponId, request.getCouponId())
                                        .eq(com.pzhu.mall.modules.marketing.entity.UserCoupon::getStatus, 0)
                                        .last("LIMIT 1")
                        );
                if (userCoupon != null) {
                    couponDiscount = couponService.calculateDiscountByUserCoupon(
                            userCoupon.getId(), goodsAmount, userId, shopId, new ArrayList<>(categoryIds));
                }
            }

            // 积分抵扣（FRONT-07 修复：仅首个分组应用一次，与下单侧 H-5 语义对齐）
            BigDecimal pointsDeduct = BigDecimal.ZERO;
            if (Boolean.TRUE.equals(request.getUsePoints()) && !pointsProcessed) {
                pointsProcessed = true;
                java.math.BigDecimal[] deductResult = pointsService.calculateDeduct(userId, goodsAmount);
                pointsDeduct = deductResult[0];
            }

            BigDecimal discountAmount = promotionDiscount.add(couponDiscount).add(pointsDeduct);
            BigDecimal payAmount = goodsAmount.add(freightAmount).subtract(discountAmount);
            // O-12 修复：实付金额负数钳制（与 OrderGroupProcessor 同口径）
            if (payAmount.compareTo(BigDecimal.ZERO) < 0) {
                payAmount = BigDecimal.ZERO;
            }

            OrderEstimateVO vo = new OrderEstimateVO();
            vo.setShopId(shopId);
            // O-12 修复：店铺名查库，不再写死"店铺"+id
            com.pzhu.mall.modules.shop.entity.Shop shop = shopMapper.selectById(shopId);
            vo.setShopName(shop != null && shop.getName() != null ? shop.getName() : "店铺" + shopId);
            vo.setGoodsAmount(goodsAmount);
            vo.setFreightAmount(freightAmount);
            vo.setPromotionDiscountAmount(promotionDiscount);
            vo.setCouponDiscountAmount(couponDiscount);
            vo.setPointsDeductAmount(pointsDeduct);
            vo.setDiscountAmount(discountAmount);
            vo.setPayAmount(payAmount);
            vo.setItems(itemVOs);
            result.add(vo);
        }

        return Result.success(result);
    }

    public static class EstimateRequest {
        private Long addressId;
        private List<ProductItemDTO> productItems;
        private Long couponId;
        private Boolean usePoints;

        public Long getAddressId() { return addressId; }
        public void setAddressId(Long addressId) { this.addressId = addressId; }
        public List<ProductItemDTO> getProductItems() { return productItems; }
        public void setProductItems(List<ProductItemDTO> productItems) { this.productItems = productItems; }
        public Long getCouponId() { return couponId; }
        public void setCouponId(Long couponId) { this.couponId = couponId; }
        public Boolean getUsePoints() { return usePoints; }
        public void setUsePoints(Boolean usePoints) { this.usePoints = usePoints; }
    }
}
