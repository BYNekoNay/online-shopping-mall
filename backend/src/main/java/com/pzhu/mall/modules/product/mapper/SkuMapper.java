package com.pzhu.mall.modules.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pzhu.mall.modules.product.entity.Sku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SkuMapper extends BaseMapper<Sku> {

    /**
     * 库存扣减（原子操作：UPDATE ... SET stock = stock - ? WHERE id = ? AND stock >= ?）。
     * 利用数据库行级锁防止并发超卖，返回 true 表示扣减成功。
     */
    boolean deductStock(@Param("skuId") Long skuId, @Param("quantity") int quantity);

    /**
     * 库存归还（原子操作：UPDATE ... SET stock = stock + ? WHERE id = ?）。
     * 用于退款审核通过后的库存恢复（O-01 修复）。
     */
    int restoreStock(@Param("skuId") Long skuId, @Param("quantity") int quantity);
}
