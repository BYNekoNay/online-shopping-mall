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
     */
    List<Order> selectTimeoutUnpaidOrders(@Param("timeoutMinutes") int timeoutMinutes,
                                         @Param("now") LocalDateTime now);

    /**
     * 统计平台累计实付金额（GMV）。
     */
    BigDecimal selectAllTotalPayAmount();
}
