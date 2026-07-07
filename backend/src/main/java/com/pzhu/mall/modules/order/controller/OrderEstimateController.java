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
        for (java.util.Map.Entry<Long, List<ProductItemDTO>> entry : byShop.entrySet()) {
            Long shopId = entry.getKey();
            List<ProductItemDTO> groupItems = entry.getValue();

            BigDecimal goodsAmount = BigDecimal.ZERO;
            List<OrderEstimateVO.ItemEstimateVO> itemVOs = new ArrayList<>();
            for (ProductItemDTO item : groupItems) {
                Product product = productMapper.selectById(item.getProductId());
                Sku sku = item.getSkuId() != null ? skuMapper.selectById(item.getSkuId()) : null;
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

            // 优惠券抵扣
            BigDecimal couponDiscount = BigDecimal.ZERO;
            if (request.getCouponId() != null) {
                couponDiscount = couponService.calculateDiscount(request.getCouponId(), goodsAmount);
            }

            // 积分抵扣
            BigDecimal pointsDeduct = BigDecimal.ZERO;
            if (Boolean.TRUE.equals(request.getUsePoints())) {
                java.math.BigDecimal[] deductResult = pointsService.calculateDeduct(userId, goodsAmount);
                pointsDeduct = deductResult[0];
            }

            BigDecimal discountAmount = promotionDiscount.add(couponDiscount).add(pointsDeduct);
            BigDecimal payAmount = goodsAmount.add(freightAmount).subtract(discountAmount);

            OrderEstimateVO vo = new OrderEstimateVO();
            vo.setShopId(shopId);
            vo.setShopName("店铺" + shopId);
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
