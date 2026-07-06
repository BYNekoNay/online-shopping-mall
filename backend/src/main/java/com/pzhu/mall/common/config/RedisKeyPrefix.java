package com.pzhu.mall.common.config;

/**
 * Redis key 前缀规范。
 * <p>
 * 统一格式：{模块}:{业务标识}:{唯一键}
 * 所有 Redis 操作必须通过本类常量拼装 key，禁止在业务代码中硬编码裸键名。
 */
public final class RedisKeyPrefix {

    private RedisKeyPrefix() {}

    /** 用户模块 */
    public static final String USER       = "mall:user";
    /** 商品模块 */
    public static final String PRODUCT    = "mall:product";
    /** 购物车模块 */
    public static final String CART       = "mall:cart";
    /** 订单模块 */
    public static final String ORDER      = "mall:order";
    /** 库存预扣减 */
    public static final String STOCK      = "mall:stock";
    /** 库存分布式锁 */
    public static final String STOCK_LOCK = "mall:lock:stock";
    /** 行为埋点 */
    public static final String BEHAVIOR   = "mall:behavior";
    /** 推荐模块 */
    public static final String RECOMMEND  = "mall:recommend";
    /** 营销模块 */
    public static final String COUPON     = "mall:coupon";
    public static final String PROMOTION  = "mall:promotion";
    /** 物流模块 */
    public static final String LOGISTICS  = "mall:logistics";
    /** 统计模块 */
    public static final String STATISTICS = "mall:statistics";
    /** 字典模块 */
    public static final String DICT       = "mall:dict";
    /** 上传模块 */
    public static final String UPLOAD     = "mall:upload";
    /** 搜索历史 */
    public static final String SEARCH_HISTORY = "mall:search:history";
}
