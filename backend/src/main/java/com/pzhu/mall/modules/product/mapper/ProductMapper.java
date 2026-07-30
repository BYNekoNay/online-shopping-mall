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
}
