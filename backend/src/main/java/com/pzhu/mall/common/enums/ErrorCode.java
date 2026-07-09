package com.pzhu.mall.common.enums;

public enum ErrorCode {

    PARAM_ERROR(10001, "参数有误，请检查后重试"),
    UNAUTHORIZED(10002, "登录已过期，请重新登录"),
    FORBIDDEN(10003, "您没有权限执行此操作"),
    NOT_FOUND(10004, "您访问的内容不存在"),
    SYSTEM_BUSY(10005, "系统繁忙，请稍后重试"),
    USERNAME_OR_PASSWORD_ERROR(20001, "用户名或密码错误"),
    ACCOUNT_DISABLED(20002, "账号已被禁用，请联系客服"),
    USERNAME_EXISTS(20003, "该用户名已被注册"),
    PRODUCT_NOT_FOUND(30001, "商品不存在或已下架"),
    PRODUCT_OFFLINE(30002, "商品已下架"),
    SKU_NOT_FOUND(30003, "商品规格不存在"),
    STOCK_NOT_ENOUGH(40001, "库存不足"),
    PRODUCT_OFFLINE_ORDER(40002, "商品已下架，无法下单"),
    COUPON_UNAVAILABLE(40003, "优惠券暂不可用"),
    ORDER_STATUS_INVALID(40004, "订单状态不支持此操作"),
    ORDER_NOT_FOUND(40005, "订单不存在"),
    PAY_FAILED(50001, "支付失败，请重试"),
    ORDER_ALREADY_PAID(50002, "订单已支付，请勿重复支付"),
    PAY_CHANNEL_ERROR(50003, "支付通道异常，请稍后重试"),
    COUPON_SOLD_OUT(60001, "优惠券已领完"),
    COUPON_EXPIRED(60002, "优惠券已过期"),
    PROMOTION_NOT_ACTIVE(60003, "促销活动已结束"),
    FREIGHT_TEMPLATE_NOT_FOUND(70001, "运费模板不存在"),
    LOGISTICS_QUERY_FAILED(70002, "物流信息查询失败"),
    RECOMMEND_GENERATING(80001, "推荐内容正在生成中，请稍后再看"),
    STATISTICS_NOT_READY(80002, "统计数据正在生成中，请稍后再看"),
    PARAM_INVALID(90001, "参数不正确"),
    DICT_KEY_DUPLICATE(90002, "配置键已存在");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
}
