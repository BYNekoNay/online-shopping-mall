package com.pzhu.mall.common.enums;

/**
 * 商品状态枚举。
 */
public enum ProductStatus {

    OFFLINE(0, "已下架"),
    ONLINE(1, "已上架"),
    PENDING(2, "待审核"),
    REJECTED(3, "审核拒绝");

    private final int code;
    private final String desc;

    ProductStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() { return code; }
    public String getDesc() { return desc; }

    public static ProductStatus of(int code) {
        for (ProductStatus s : values()) {
            if (s.code == code) return s;
        }
        return null;
    }
}
