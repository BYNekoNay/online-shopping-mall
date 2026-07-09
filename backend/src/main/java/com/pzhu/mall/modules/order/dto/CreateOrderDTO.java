package com.pzhu.mall.modules.order.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 创建订单请求 DTO。
 */
public class CreateOrderDTO {

    /**
     * 幂等请求 ID（前端生成 UUID）。
     */
    private String requestId;

    /**
     * 收货地址 ID。
     */
    private Long addressId;

    /**
     * 购物车项 ID 列表（与 productItems 二选一）。
     */
    private List<Long> cartItemIds;

    /**
     * 直接购买的商品项（与 cartItemIds 二选一）。
     */
    private List<ProductItemDTO> productItems;

    /**
     * 用户优惠券记录 ID（UserCoupon 表主键，可选）。
     */
    private Long userCouponId;

    /**
     * 是否使用积分抵扣。
     */
    private Boolean usePoints;

    /**
     * 用户备注。
     */
    private String remark;

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public Long getAddressId() { return addressId; }
    public void setAddressId(Long addressId) { this.addressId = addressId; }
    public List<Long> getCartItemIds() { return cartItemIds; }
    public void setCartItemIds(List<Long> cartItemIds) { this.cartItemIds = cartItemIds; }
    public List<ProductItemDTO> getProductItems() { return productItems; }
    public void setProductItems(List<ProductItemDTO> productItems) { this.productItems = productItems; }
    public Long getUserCouponId() { return userCouponId; }
    public void setUserCouponId(Long userCouponId) { this.userCouponId = userCouponId; }
    public Boolean getUsePoints() { return usePoints; }
    public void setUsePoints(Boolean usePoints) { this.usePoints = usePoints; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
