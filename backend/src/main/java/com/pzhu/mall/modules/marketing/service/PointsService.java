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
            log.warn("[积分] 扣回失败：用户积分不足 userId={} amount={}", record.getUserId(), clawbackAmount);
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
     * 查询积分变动流水（分页，使用 MyBatis-Plus selectPage 避免 OOM）。
     */
    public Page<PointsRecord> listRecords(Long userId, int pageNum, int pageSize) {
        Page<PointsRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<PointsRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(PointsRecord::getUserId, userId)
          .orderByDesc(PointsRecord::getCreateTime);
        return pointsRecordMapper.selectPage(page, qw);
    }
}
