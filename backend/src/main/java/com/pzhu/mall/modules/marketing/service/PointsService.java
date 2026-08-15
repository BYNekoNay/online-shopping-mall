package com.pzhu.mall.modules.marketing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.modules.user.entity.User;
import com.pzhu.mall.modules.user.mapper.UserMapper;
import com.pzhu.mall.modules.marketing.entity.PointsRecord;
import com.pzhu.mall.modules.marketing.mapper.PointsRecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 积分服务。
 */
@Service
public class PointsService {

    private static final Logger log = LoggerFactory.getLogger(PointsService.class);

    @Resource
    private UserMapper userMapper;

    @Resource
    private PointsRecordMapper pointsRecordMapper;

    /**
     * 计算积分可抵扣额度。
     * 规则：min(points/100, goodsAmount*0.5)，向下取整到分。
     *
     * @return [0]=可抵扣金额, [1]=所需积分
     */
    public BigDecimal[] calculateDeduct(Long userId, BigDecimal goodsAmount) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO};
        }
        int points = user.getPoints() != null ? user.getPoints() : 0;
        BigDecimal byPoints = new BigDecimal(points).divide(new BigDecimal(100), 2, RoundingMode.DOWN);
        BigDecimal byAmount = goodsAmount.multiply(new BigDecimal("0.5"));
        BigDecimal deduct = byPoints.min(byAmount);
        int pointsNeeded = deduct.multiply(new BigDecimal(100)).intValue();
        return new BigDecimal[]{deduct, new BigDecimal(pointsNeeded)};
    }

    /**
     * 下单时扣除积分（原子更新防并发）。
     * <p>使用 {@code UPDATE user SET points = points - ? WHERE id = ? AND points >= ?}
     * 原子操作，防止并发扣减导致积分变为负数。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void settleDeduct(Long userId, int pointsUsed, Long orderId) {
        if (pointsUsed <= 0) return;

        // 原子扣减：points = points - pointsUsed，仅当 points >= pointsUsed 时成功
        int updated = userMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<com.pzhu.mall.modules.user.entity.User>()
                        .setSql("points = points - " + pointsUsed)
                        .eq(com.pzhu.mall.modules.user.entity.User::getId, userId)
                        .ge(com.pzhu.mall.modules.user.entity.User::getPoints, pointsUsed)
        );
        if (updated == 0) {
            // 检查用户是否存在
            com.pzhu.mall.modules.user.entity.User user = userMapper.selectById(userId);
            if (user == null) return;
            throw new BusinessException(ErrorCode.PARAM_ERROR, "积分不足，无法抵扣");
        }

        PointsRecord record = new PointsRecord();
        record.setUserId(userId);
        record.setChangeAmount(-pointsUsed);
        record.setType(2); // 订单抵扣
        record.setRelatedOrderId(orderId);
        record.setCreateTime(LocalDateTime.now());
        pointsRecordMapper.insert(record);
        log.info("[积分] 用户={} 订单={} 抵扣积分={}", userId, orderId, pointsUsed);
    }

    /**
     * 支付成功后发放积分（按实付金额 1:1，原子 UPDATE 防并发丢失）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void settleEarn(Long orderId, Long userId, BigDecimal payAmount) {
        // M9 修复：ROUND_DOWN → HALF_UP，对用户更公平
        int points = payAmount.setScale(0, BigDecimal.ROUND_HALF_UP).intValue();
        if (points <= 0) return;

        // C3 修复：原子 UPDATE 替代读-改-写，防止并发丢失
        userMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<User>()
                        .setSql("points = points + " + points)
                        .eq(User::getId, userId)
        );

        PointsRecord record = new PointsRecord();
        record.setUserId(userId);
        record.setChangeAmount(points);
        record.setType(1); // 下单获取
        record.setRelatedOrderId(orderId);
        // C-2：下单获取积分 365 天有效期（简化 FIFO：过期时按"获取量总和"近似扣减）
        record.setExpireTime(LocalDateTime.now().plusDays(365));
        record.setCreateTime(LocalDateTime.now());
        pointsRecordMapper.insert(record);
        log.info("[积分] 用户={} 订单={} 获得积分={}", userId, orderId, points);
    }

    /**
     * 退款时扣回积分（原子 UPDATE 防并发丢失）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void clawback(Long orderId) {
        List<PointsRecord> records = pointsRecordMapper.selectList(
                new LambdaQueryWrapper<PointsRecord>()
                        .eq(PointsRecord::getRelatedOrderId, orderId)
                        .eq(PointsRecord::getType, 1)
                        .last("LIMIT 1")
        );
        if (records.isEmpty()) return;
        PointsRecord record = records.get(0);
        int clawbackAmount = record.getChangeAmount() != null ? record.getChangeAmount() : 0;
        if (clawbackAmount <= 0) return;

        // C3 修复：原子 UPDATE 替代读-改-写，WHERE points >= ? 防负数
        int updated = userMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<User>()
                        .setSql("points = GREATEST(points - " + clawbackAmount + ", 0)")
                        .eq(User::getId, record.getUserId())
                        .ge(User::getPoints, clawbackAmount)
        );
        if (updated == 0) {
            // H-11 修复：扣减失败（积分不足）时不再插入扣减流水，避免账实不符
            log.warn("[积分] 扣回失败：用户积分不足，跳过扣减流水 userId={} amount={}", record.getUserId(), clawbackAmount);
            return;
        }

        PointsRecord clawback = new PointsRecord();
        clawback.setUserId(record.getUserId());
        clawback.setChangeAmount(-clawbackAmount);
        clawback.setType(3); // 兑换/扣回
        clawback.setRelatedOrderId(orderId);
        clawback.setCreateTime(LocalDateTime.now());
        pointsRecordMapper.insert(clawback);
        log.info("[积分] 订单={} 退款扣回积分={} 用户={}", orderId, clawbackAmount, record.getUserId());
    }

    /**
     * H-5 修复：返还下单时抵扣的积分（type=2 记录）。
     * <p>用于取消未支付订单、退款审核通过两个场景。原 {@link #clawback(Long)} 仅处理
     * type=1（下单获取）记录，未支付订单取消时抵扣的积分无人返还，造成积分永久丢失。</p>
     * <p>幂等设计：同一订单已存在 type=4（取消/退款返还）记录时直接跳过，
     * 防止重复取消、重复审核等场景下积分被多次返还。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void refundDeduct(Long orderId) {
        // 幂等守卫：已返还过则跳过
        Long returned = pointsRecordMapper.selectCount(
                new LambdaQueryWrapper<PointsRecord>()
                        .eq(PointsRecord::getRelatedOrderId, orderId)
                        .eq(PointsRecord::getType, 4)
        );
        if (returned != null && returned > 0) {
            return;
        }

        List<PointsRecord> records = pointsRecordMapper.selectList(
                new LambdaQueryWrapper<PointsRecord>()
                        .eq(PointsRecord::getRelatedOrderId, orderId)
                        .eq(PointsRecord::getType, 2)
                        .last("LIMIT 1")
        );
        if (records.isEmpty()) return;
        PointsRecord record = records.get(0);
        // 抵扣流水的 changeAmount 记为负数，取绝对值返还
        int amount = Math.abs(record.getChangeAmount() != null ? record.getChangeAmount() : 0);
        if (amount <= 0) return;

        // 原子返还：points = points + amount（返还不会为负，无需下限守卫）
        userMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<User>()
                        .setSql("points = points + " + amount)
                        .eq(User::getId, record.getUserId())
        );

        PointsRecord refund = new PointsRecord();
        refund.setUserId(record.getUserId());
        refund.setChangeAmount(amount);
        refund.setType(4); // 取消/退款返还（H-5 修复新增类型）
        refund.setRelatedOrderId(orderId);
        refund.setCreateTime(LocalDateTime.now());
        pointsRecordMapper.insert(refund);
        log.info("[积分] 订单={} 取消/退款返还抵扣积分={} 用户={}", orderId, amount, record.getUserId());
    }

    /**
     * 查询积分变动流水（分页，使用 MyBatis-Plus selectPage 避免 OOM）。
     */
    public Page<PointsRecord> listRecords(Long userId, int pageNum, int pageSize) {
        Page<PointsRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<PointsRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(PointsRecord::getUserId, userId)
          .orderByDesc(PointsRecord::getCreateTime);
        return pointsRecordMapper.selectPage(page, qw);
    }

    // ==================== C-2 积分有效期 ====================

    /** 积分有效期（天）：下单获取的积分 365 天后过期。 */
    public static final int POINTS_VALID_DAYS = 365;

    /**
     * 获取即将过期积分（30 天内到期，type=1 获取记录之和），供前端"即将过期"提示。
     */
    public int getExpiringPoints(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        List<PointsRecord> records = pointsRecordMapper.selectList(
                new LambdaQueryWrapper<PointsRecord>()
                        .eq(PointsRecord::getUserId, userId)
                        .eq(PointsRecord::getType, 1)
                        .isNotNull(PointsRecord::getExpireTime)
                        .le(PointsRecord::getExpireTime, now.plusDays(30))
                        .ge(PointsRecord::getExpireTime, now));
        return records.stream()
                .mapToInt(r -> r.getChangeAmount() != null ? Math.max(r.getChangeAmount(), 0) : 0)
                .sum();
    }

    /**
     * C-2 定时任务：清理过期积分（每日 03:00）。
     *
     * <p>简化 FIFO：将"已过期获取记录"（expired=0 且 expire_time<now）按用户汇总，
     * 从用户积分中扣减（GREATEST 不低于 0），随后将记录标记 expired=1（幂等，防重复扣减清掉新积分）。
     * 真实 FIFO 需记录"消耗映射"，毕设场景采用近似实现并文档声明。</p>
     */
    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 3 * * ?")
    public void expirePoints() {
        LocalDateTime now = LocalDateTime.now();
        // 查所有已过期且未清理的获取记录
        List<PointsRecord> expired = pointsRecordMapper.selectList(
                new LambdaQueryWrapper<PointsRecord>()
                        .eq(PointsRecord::getType, 1)
                        .eq(PointsRecord::getExpired, 0)
                        .isNotNull(PointsRecord::getExpireTime)
                        .lt(PointsRecord::getExpireTime, now));
        if (expired.isEmpty()) {
            return;
        }
        // 按用户汇总过期量
        java.util.Map<Long, Integer> expiredByUser = new java.util.HashMap<>();
        java.util.List<Long> expiredIds = new java.util.ArrayList<>();
        for (PointsRecord r : expired) {
            int amount = r.getChangeAmount() != null ? Math.max(r.getChangeAmount(), 0) : 0;
            expiredByUser.merge(r.getUserId(), amount, Integer::sum);
            expiredIds.add(r.getId());
        }
        int cleared = 0;
        for (java.util.Map.Entry<Long, Integer> e : expiredByUser.entrySet()) {
            int updated = userMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<User>()
                            .setSql("points = GREATEST(points - " + e.getValue() + ", 0)")
                            .eq(User::getId, e.getKey()));
            if (updated > 0) {
                cleared += e.getValue();
            }
        }
        // 标记已清理（幂等：再次运行不再匹配 expired=0）
        if (!expiredIds.isEmpty()) {
            pointsRecordMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<PointsRecord>()
                            .set(PointsRecord::getExpired, 1)
                            .in(PointsRecord::getId, expiredIds));
        }
        log.info("[积分-过期] 清理 {} 个用户过期积分，累计 {} 分", expiredByUser.size(), cleared);
    }
}
