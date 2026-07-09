package com.pzhu.mall.modules.statistics.service;

import com.pzhu.mall.modules.behavior.entity.PageViewLog;
import com.pzhu.mall.modules.behavior.entity.UserBehavior;
import com.pzhu.mall.modules.behavior.mapper.PageViewLogMapper;
import com.pzhu.mall.modules.behavior.mapper.UserBehaviorMapper;
import com.pzhu.mall.modules.cart.entity.Cart;
import com.pzhu.mall.modules.cart.mapper.CartMapper;
import com.pzhu.mall.modules.order.entity.Order;
import com.pzhu.mall.modules.order.mapper.OrderMapper;
import com.pzhu.mall.modules.recommend.entity.RecommendResult;
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
import java.util.stream.Collectors;

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

        // 推荐点击率（近7天）— 使用 selectCount 聚合查询
        LocalDateTime weekAgo = now.minusDays(7);
        long exposureCount = recommendResultMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RecommendResult>()
                        .ge(RecommendResult::getGenerateTime, weekAgo)
        );
        long clickCount = userBehaviorMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserBehavior>()
                        .eq(UserBehavior::getBehaviorType, 1)
                        .ge(UserBehavior::getCreateTime, weekAgo)
        );
        String recommendCtr = exposureCount > 0 ? String.format("%.2f%%", clickCount * 100.0 / exposureCount) : "0.00%";

        // 转化率（浏览商品用户中下单用户占比）— R3-C3: 用 selectList + in-memory distinct 替代 selectCount 去重
        List<UserBehavior> browseBehaviors = userBehaviorMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserBehavior>()
                        .eq(UserBehavior::getBehaviorType, 1)
                        .select(UserBehavior::getUserId)
        );
        long browseUserCount = browseBehaviors.stream()
                .map(UserBehavior::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        List<Order> orderUsers = orderMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order>()
                        .eq(Order::getIsDeleted, 0)
                        .select(Order::getUserId)
        );
        long orderUserCount = orderUsers.stream()
                .map(Order::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
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
        // UV 需要去重，用 selectList 仅查 userId 字段（数据量通常不大，且分页范围有限）
        List<PageViewLog> logs = pageViewLogMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PageViewLog>()
                        .ge(PageViewLog::getEnterTime, start)
                        .le(PageViewLog::getEnterTime, end)
                        .select(PageViewLog::getUserId, PageViewLog::getSessionId, PageViewLog::getStayDuration)
        );
        int uv = (int) logs.stream()
                .map(PageViewLog::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        // 跳出率：session 内仅有 1 条记录的会话数 / 总会话数
        Map<String, Long> sessionCounts = logs.stream()
                .collect(Collectors.groupingBy(PageViewLog::getSessionId, Collectors.counting()));
        long bounceSessions = sessionCounts.values().stream().filter(c -> c == 1).count();
        double bounceRate = sessionCounts.isEmpty() ? 0 : (double) bounceSessions / sessionCounts.size();

        // 平均停留时长（秒）
        double avgStay = logs.stream()
                .filter(l -> l.getStayDuration() != null)
                .mapToInt(PageViewLog::getStayDuration)
                .average()
                .orElse(0.0);
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
}
