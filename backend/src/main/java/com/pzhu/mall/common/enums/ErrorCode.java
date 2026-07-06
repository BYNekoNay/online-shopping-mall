package com.pzhu.mall.common.enums;

public enum ErrorCode {

    PARAM_ERROR(10001, "Parameter validation failed"),
    UNAUTHORIZED(10002, "Not logged in or session expired"),
    FORBIDDEN(10003, "Permission denied"),
    NOT_FOUND(10004, "Resource not found"),
    SYSTEM_BUSY(10005, "System busy, please try again later"),
    USERNAME_OR_PASSWORD_ERROR(20001, "Username or password is incorrect"),
    ACCOUNT_DISABLED(20002, "Account has been disabled"),
    USERNAME_EXISTS(20003, "Username already exists"),
    PRODUCT_NOT_FOUND(30001, "Product not found"),
    PRODUCT_OFFLINE(30002, "Product is currently offline"),
    SKU_NOT_FOUND(30003, "SKU not found"),
    STOCK_NOT_ENOUGH(40001, "Insufficient stock"),
    PRODUCT_OFFLINE_ORDER(40002, "Product is offline"),
    COUPON_UNAVAILABLE(40003, "Coupon is not available"),
    ORDER_STATUS_INVALID(40004, "Order status does not allow this operation"),
    ORDER_NOT_FOUND(40005, "Order not found"),
    PAY_FAILED(50001, "Payment failed"),
    ORDER_ALREADY_PAID(50002, "Order has already been paid"),
    PAY_CHANNEL_ERROR(50003, "Payment channel error"),
    COUPON_SOLD_OUT(60001, "Coupon has been claimed out"),
    COUPON_EXPIRED(60002, "Coupon has expired"),
    PROMOTION_NOT_ACTIVE(60003, "Promotion is not active"),
    FREIGHT_TEMPLATE_NOT_FOUND(70001, "Freight template not found"),
    LOGISTICS_QUERY_FAILED(70002, "Logistics query failed"),
    RECOMMEND_GENERATING(80001, "Recommendation is being generated"),
    STATISTICS_NOT_READY(80002, "Statistics data not yet generated"),
    PARAM_INVALID(90001, "Parameter is invalid"),
    DICT_KEY_DUPLICATE(90002, "Dictionary key already exists");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
}
