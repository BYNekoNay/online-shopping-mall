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
     * 下单时扣除积分。
     * <p>校验 pointsUsed 不超过用户当前可用积分，防止扣成负数。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void settleDeduct(Long userId, int pointsUsed, Long orderId) {
        User user = userMapper.selectById(userId);
        if (user == null) return;
        int currentPoints = user.getPoints() != null ? user.getPoints() : 0;
        if (pointsUsed > currentPoints) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "积分不足，无法抵扣");
        }
        user.setPoints(currentPoints - pointsUsed);
        userMapper.updateById(user);

        PointsRecord record = new PointsRecord();
        record.setUserId(userId);
        record.setChangeAmount(-pointsUsed);
        record.setType(2); // 订单抵扣
        record.setRelatedOrderId(orderId);
        record.setCreateTime(LocalDateTime.now());
        pointsRecordMapper.insert(record);
        log.info("[积分] 用户={} 订单={} 抵扣积分={} 剩余={}", userId, orderId, pointsUsed, currentPoints - pointsUsed);
    }

    /**
     * 支付成功后发放积分（按实付金额 1:1）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void settleEarn(Long orderId, Long userId, BigDecimal payAmount) {
        int points = payAmount.setScale(0, BigDecimal.ROUND_DOWN).intValue();
        User user = userMapper.selectById(userId);
        if (user == null) return;
        int currentPoints = user.getPoints() != null ? user.getPoints() : 0;
        user.setPoints(currentPoints + points);
        userMapper.updateById(user);

        PointsRecord record = new PointsRecord();
        record.setUserId(userId);
        record.setChangeAmount(points);
        record.setType(1); // 下单获取
        record.setRelatedOrderId(orderId);
        record.setCreateTime(LocalDateTime.now());
        pointsRecordMapper.insert(record);
        log.info("[积分] 用户={} 订单={} 获得积分={} 累计={}", userId, orderId, points, currentPoints + points);
    }

    /**
     * 退款时扣回积分。
     */
    @Transactional(rollbackFor = Exception.class)
    public void clawback(Long orderId) {
        PointsRecord record = pointsRecordMapper.selectOne(
                new LambdaQueryWrapper<PointsRecord>()
                        .eq(PointsRecord::getRelatedOrderId, orderId)
                        .eq(PointsRecord::getType, 1)
                        .last("LIMIT 1")
        );
        if (record == null) return;
        User user = userMapper.selectById(record.getUserId());
        if (user == null) return;
        int currentPoints = user.getPoints() != null ? user.getPoints() : 0;
        int clawbackAmount = record.getChangeAmount() != null ? record.getChangeAmount() : 0;
        int newPoints = Math.max(0, currentPoints - clawbackAmount);
        user.setPoints(newPoints);
        userMapper.updateById(user);

        PointsRecord clawback = new PointsRecord();
        clawback.setUserId(record.getUserId());
        clawback.setChangeAmount(-clawbackAmount);
        clawback.setType(3); // 兑换/扣回
        clawback.setRelatedOrderId(orderId);
        clawback.setCreateTime(LocalDateTime.now());
        pointsRecordMapper.insert(clawback);
        log.info("[积分] 订单={} 退款扣回积分={} 用户={} 剩余={}", orderId, clawbackAmount, record.getUserId(), newPoints);
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
