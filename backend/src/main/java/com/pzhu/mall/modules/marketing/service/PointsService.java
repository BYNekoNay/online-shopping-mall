package com.pzhu.mall.modules.marketing.service;

import com.pzhu.mall.modules.user.entity.User;
import com.pzhu.mall.modules.user.mapper.UserMapper;
import com.pzhu.mall.modules.marketing.entity.PointsRecord;
import com.pzhu.mall.modules.marketing.mapper.PointsRecordMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 积分服务。
 */
@Service
public class PointsService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private PointsRecordMapper pointsRecordMapper;

    /**
     * 计算积分可抵扣额度。
     * 规则：min(points/100, goodsAmount*0.5)，向下取整到分。
     */
    public BigDecimal[] calculateDeduct(Long userId, BigDecimal goodsAmount) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO};
        }
        int points = user.getPoints();
        BigDecimal byPoints = new BigDecimal(points).divide(new BigDecimal(100), 2, BigDecimal.ROUND_DOWN);
        BigDecimal byAmount = goodsAmount.multiply(new BigDecimal("0.5"));
        BigDecimal deduct = byPoints.min(byAmount);
        int pointsNeeded = deduct.multiply(new BigDecimal(100)).intValue();
        return new BigDecimal[]{deduct, new BigDecimal(pointsNeeded)};
    }

    /**
     * 下单时扣除积分。
     */
    public void settleDeduct(Long userId, int pointsUsed, Long orderId) {
        User user = userMapper.selectById(userId);
        if (user == null) return;
        int newPoints = Math.max(0, user.getPoints() - pointsUsed);
        user.setPoints(newPoints);
        userMapper.updateById(user);

        PointsRecord record = new PointsRecord();
        record.setUserId(userId);
        record.setChangeAmount(-pointsUsed);
        record.setType(2); // 订单抵扣
        record.setRelatedOrderId(orderId);
        record.setCreateTime(LocalDateTime.now());
        pointsRecordMapper.insert(record);
    }

    /**
     * 支付成功后发放积分（按实付金额 1:1）。
     */
    public void settleEarn(Long orderId, Long userId, BigDecimal payAmount) {
        int points = payAmount.setScale(0, BigDecimal.ROUND_DOWN).intValue();
        User user = userMapper.selectById(userId);
        if (user == null) return;
        user.setPoints(user.getPoints() + points);
        userMapper.updateById(user);

        PointsRecord record = new PointsRecord();
        record.setUserId(userId);
        record.setChangeAmount(points);
        record.setType(1); // 下单获取
        record.setRelatedOrderId(orderId);
        record.setCreateTime(LocalDateTime.now());
        pointsRecordMapper.insert(record);
    }

    /**
     * 退款时扣回积分。
     */
    public void clawback(Long orderId) {
        PointsRecord record = pointsRecordMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PointsRecord>()
                .eq(PointsRecord::getRelatedOrderId, orderId)
                .eq(PointsRecord::getType, 1)
                .last("LIMIT 1")
        );
        if (record == null) return;
        User user = userMapper.selectById(record.getUserId());
        if (user == null) return;
        int newPoints = Math.max(0, user.getPoints() - record.getChangeAmount());
        user.setPoints(newPoints);
        userMapper.updateById(user);

        PointsRecord clawback = new PointsRecord();
        clawback.setUserId(record.getUserId());
        clawback.setChangeAmount(-record.getChangeAmount());
        clawback.setType(3); // 兑换/扣回
        clawback.setRelatedOrderId(orderId);
        clawback.setCreateTime(LocalDateTime.now());
        pointsRecordMapper.insert(clawback);
    }
}
