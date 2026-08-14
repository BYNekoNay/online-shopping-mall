package com.pzhu.mall.modules.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pzhu.mall.modules.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * H-4 修复：无 SKU 商品的库存扣减（原子操作：UPDATE ... SET stock = stock - ? WHERE id = ? AND stock >= ?）。
     * <p>原实现仅在 skuId != null 分支扣减 SKU 库存，单规格商品（直接使用 product.stock）
     * 在下单、支付全链路零库存控制，可被无限超卖。</p>
     */
    boolean deductStock(@Param("productId") Long productId, @Param("quantity") int quantity);

    /**
     * 库存归还（原子操作：UPDATE ... SET stock = stock + ? WHERE id = ?）。
     * 用于退款审核通过后的库存恢复（O-01 修复）。
     */
    int restoreStock(@Param("productId") Long productId, @Param("quantity") int quantity);

    /**
     * O-04 修复：商品总库存的原子扣减（GREATEST 防负），
     * 用于 SKU 商品支付时同步 product.stock，替代"select + set + updateById"非原子读改写。
     * 与 deductStock 的区别：不做 stock >= ? 条件判断（sku 已原子扣减，product.stock 为汇总冗余）。
     */
    int deductStockUnchecked(@Param("productId") Long productId, @Param("quantity") int quantity);
}
