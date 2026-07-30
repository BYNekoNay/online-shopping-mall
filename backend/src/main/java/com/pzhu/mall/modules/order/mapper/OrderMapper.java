package com.pzhu.mall.modules.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pzhu.mall.modules.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 查询指定时间之前未支付的订单列表（供定时取消任务使用）。
     * 利用 idx_status_create_time 复合索引避免全表扫描。
     * <p>H-9 修复：阈值由调用方计算一次后直接传入；原实现 Java 侧 minusMinutes
     * 与 SQL 侧 DATE_SUB 各减一次，导致实际超时时间被双重减半。</p>
     */
    List<Order> selectTimeoutUnpaidOrders(@Param("threshold") LocalDateTime threshold);

    /**
     * 统计平台累计实付金额（GMV）。
     */
    BigDecimal selectAllTotalPayAmount();
}
