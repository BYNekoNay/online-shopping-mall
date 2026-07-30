package com.pzhu.mall.modules.statistics.service;

import com.pzhu.mall.modules.behavior.entity.PageViewLog;
import com.pzhu.mall.modules.behavior.entity.UserBehavior;
import com.pzhu.mall.modules.behavior.mapper.PageViewLogMapper;
import com.pzhu.mall.modules.behavior.mapper.UserBehaviorMapper;
import com.pzhu.mall.modules.cart.entity.Cart;
import com.pzhu.mall.modules.cart.mapper.CartMapper;
import com.pzhu.mall.modules.order.entity.Order;
import com.pzhu.mall.modules.order.mapper.OrderMapper;
import com.pzhu.mall.modules.recommend.mapper.RecommendResultMapper;
import com.pzhu.mall.modules.user.entity.User;
import com.pzhu.mall.modules.user.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 平台级数据统计服务。
 */
@Service
public class PlatformStatisticsService {

    private static final Logger log = LoggerFactory.getLogger(PlatformStatisticsService.class);

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private UserBehaviorMapper userBehaviorMapper;

    @Resource
    private PageViewLogMapper pageViewLogMapper;

    @Resource
    private RecommendResultMapper recommendResultMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private CartMapper cartMapper;

    /**
     * 平台看板总览指标。
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboard() {
        log.info("[平台统计] 看板总览");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();

        // GMV（累计订单实付金额）
        BigDecimal gmv = orderMapper.selectAllTotalPayAmount();

        // 今日订单数
        long todayOrders = orderMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order>()
                        .eq(Order::getIsDeleted, 0)
                        .ge(Order::getCreateTime, dayStart)
        );

        // 今日新增用户
        long newUserCount = userMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .ge(User::getCreateTime, dayStart)
        );

        // 推荐点击率（近7天）
        // M-13 修复：分子分母改用统一推荐归因口径——分母为去重推荐曝光（用户,商品）对数，
        // 分子为"先被推荐、后被浏览"的去重对数；原实现用全站浏览量除以推荐生成数，口径不一致导致指标严重虚高
        LocalDateTime weekAgo = now.minusDays(7);
        long exposureCount = recommendResultMapper.countDistinctExposure(weekAgo);
        long clickCount = recommendResultMapper.countDistinctRecommendClick(weekAgo);
        String recommendCtr = exposureCount > 0 ? String.format("%.2f%%", clickCount * 100.0 / exposureCount) : "0.00%";

        // 转化率（浏览商品用户中下单用户占比）
        // H-21 修复：原实现 selectList 全表加载进内存做 distinct，数据量大时 OOM；改为 SQL COUNT(DISTINCT) 聚合
        long browseUserCount = firstLong(userBehaviorMapper.selectObjs(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserBehavior>()
                        .select("COUNT(DISTINCT user_id)")
                        .eq("behavior_type", 1)
        ));
        long orderUserCount = firstLong(orderMapper.selectObjs(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Order>()
                        .select("COUNT(DISTINCT user_id)")
                        .eq("is_deleted", 0)
        ));
        double conversionRate = browseUserCount == 0 ? 0 :
                (double) orderUserCount / browseUserCount;

        Map<String, Object> result = new HashMap<>();
        result.put("gmv", gmv);
        result.put("orderCount", todayOrders);
        result.put("newUserCount", newUserCount);
        result.put("conversionRate", BigDecimal.valueOf(conversionRate).setScale(4, RoundingMode.HALF_UP).doubleValue());
        result.put("recommendCtr", recommendCtr);
        return result;
    }

    /**
     * 细项统计（PV/UV/跳出率/停留时长/转化漏斗）。
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getStatisticsDetail(LocalDate startDate, LocalDate endDate) {
        log.info("[平台统计] 细项统计 range={}~{}", startDate, endDate);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        // PV / UV — 使用 selectCount 统计
        long pv = pageViewLogMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PageViewLog>()
                        .ge(PageViewLog::getEnterTime, start)
                        .le(PageViewLog::getEnterTime, end)
        );
        // H-21 修复：UV 改为 SQL COUNT(DISTINCT) 聚合，不再全量加载日志进内存
        long uv = firstLong(pageViewLogMapper.selectObjs(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<PageViewLog>()
                        .select("COUNT(DISTINCT user_id)")
                        .ge("enter_time", start)
                        .le("enter_time", end)
        ));

        // 跳出率：session 内仅有 1 条记录的会话数 / 总会话数
        // H-21 修复：改为 GROUP BY session_id 聚合（返回行数=会话数，远小于日志总行数），
        // 同时避免原 groupingBy 在 sessionId 为 null 时抛 NPE
        List<Map<String, Object>> sessionRows = pageViewLogMapper.selectMaps(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<PageViewLog>()
                        .select("session_id AS sessionId", "COUNT(*) AS cnt")
                        .ge("enter_time", start)
                        .le("enter_time", end)
                        .isNotNull("session_id")
                        .groupBy("session_id")
        );
        long totalSessions = sessionRows.size();
        long bounceSessions = sessionRows.stream()
                .filter(m -> m.get("cnt") != null && ((Number) m.get("cnt")).longValue() == 1)
                .count();
        double bounceRate = totalSessions == 0 ? 0 : (double) bounceSessions / totalSessions;

        // 平均停留时长（秒）— H-21 修复：改为 SQL AVG 聚合（自动忽略 NULL，与原过滤语义一致）
        List<Object> avgObjs = pageViewLogMapper.selectObjs(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<PageViewLog>()
                        .select("AVG(stay_duration)")
                        .ge("enter_time", start)
                        .le("enter_time", end)
        );
        double avgStay = (avgObjs == null || avgObjs.isEmpty() || avgObjs.get(0) == null)
                ? 0.0 : ((Number) avgObjs.get(0)).doubleValue();
        int avgStayDuration = (int) Math.round(avgStay);

        // 转化漏斗（使用聚合查询）
        long viewCount = userBehaviorMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserBehavior>()
                        .eq(UserBehavior::getBehaviorType, 1)
                        .ge(UserBehavior::getCreateTime, start)
                        .le(UserBehavior::getCreateTime, end)
        );

        long orderCount = orderMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order>()
                        .eq(Order::getIsDeleted, 0)
                        .ge(Order::getCreateTime, start)
                        .le(Order::getCreateTime, end)
        );
        long payCount = orderMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order>()
                        .eq(Order::getIsDeleted, 0)
                        .ge(Order::getCreateTime, start)
                        .le(Order::getCreateTime, end)
                        .isNotNull(Order::getPayTime)
        );

        // 购物车人数：从购物车表真实统计
        long cartCount = cartMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Cart>()
                        .ge(Cart::getCreateTime, start)
                        .le(Cart::getCreateTime, end)
        );

        Map<String, Object> result = new HashMap<>();
        result.put("pv", pv);
        result.put("uv", uv);
        result.put("bounceRate", BigDecimal.valueOf(bounceRate).setScale(2, RoundingMode.HALF_UP).doubleValue());
        result.put("avgStayDuration", avgStayDuration);
        Map<String, Object> funnel = new HashMap<>();
        funnel.put("view", viewCount);
        funnel.put("cart", cartCount);
        funnel.put("order", orderCount);
        funnel.put("pay", payCount);
        result.put("funnel", funnel);
        return result;
    }

    /**
     * H-21 修复：从 selectObjs 聚合查询结果中安全取出单个数值（空结果返回 0）。
     */
    private static long firstLong(List<Object> objs) {
        if (objs == null || objs.isEmpty() || objs.get(0) == null) {
            return 0L;
        }
        return ((Number) objs.get(0)).longValue();
    }
}
