package com.pzhu.mall.modules.logistics.service;

import org.springframework.stereotype.Service;

/**
 * 物流查询服务（模拟实现）。
 */
@Service
public class LogisticsQueryService {

    /**
     * 查询物流轨迹（模拟返回）。
     */
    public String query(Long orderId) {
        // TODO: 对接第三方物流查询 API
        return "{\"status\":\"查询中\",\"tracks\":[{\"time\":\"2026-07-06 10:00:00\",\"desc\":\"快件已发货\"}]}";
    }
}
