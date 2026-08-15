package com.pzhu.mall.modules.marketing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.common.result.PageResult;
import com.pzhu.mall.modules.marketing.entity.PointsExchangeLog;
import com.pzhu.mall.modules.marketing.entity.PointsGoods;
import com.pzhu.mall.modules.marketing.entity.PointsRecord;
import com.pzhu.mall.modules.marketing.mapper.PointsExchangeLogMapper;
import com.pzhu.mall.modules.marketing.mapper.PointsGoodsMapper;
import com.pzhu.mall.modules.marketing.mapper.PointsRecordMapper;
import com.pzhu.mall.modules.user.entity.User;
import com.pzhu.mall.modules.user.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 积分商城服务（C-1 积分兑换商品）。
 *
 * <p>兑换流程：校验商品/库存（原子扣减）→ 校验并扣积分（原子，不足回滚库存）→
 * 写积分流水(type=5) + 兑换记录快照。整体同一事务，任一步异常自动回滚。</p>
 */
@Service
public class PointsExchangeService {

    private static final Logger log = LoggerFactory.getLogger(PointsExchangeService.class);

    @Resource
    private PointsGoodsMapper pointsGoodsMapper;

    @Resource
    private PointsExchangeLogMapper pointsExchangeLogMapper;

    @Resource
    private PointsRecordMapper pointsRecordMapper;

    @Resource
    private UserMapper userMapper;

    // ==================== 消费者 ====================

    /** 兑换商品分页列表（仅上架）。 */
    public PageResult<PointsGoods> listGoods(int pageNum, int pageSize) {
        Page<PointsGoods> page = new Page<>(pageNum, pageSize);
        pointsGoodsMapper.selectPage(page,
                new LambdaQueryWrapper<PointsGoods>()
                        .eq(PointsGoods::getStatus, 1)
                        .eq(PointsGoods::getIsDeleted, 0)
                        .orderByDesc(PointsGoods::getCreateTime));
        return new PageResult<>(page.getTotal(), pageNum, pageSize,
                (long) Math.ceil((double) page.getTotal() / pageSize), page.getRecords());
    }

    /**
     * 兑换商品（原子扣库存 + 原子扣积分，不足回滚）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void exchange(Long userId, Long goodsId, int quantity) {
        if (quantity <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "兑换数量必须大于0");
        }
        PointsGoods goods = pointsGoodsMapper.selectById(goodsId);
        if (goods == null || !Integer.valueOf(1).equals(goods.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "兑换商品不存在或已下架");
        }
        // 1. 原子扣库存（WHERE status=1 AND stock>=quantity 防并发超兑）
        int updated = pointsGoodsMapper.update(null,
                new LambdaUpdateWrapper<PointsGoods>()
                        .setSql("stock = stock - " + quantity)
                        .eq(PointsGoods::getId, goodsId)
                        .eq(PointsGoods::getStatus, 1)
                        .ge(PointsGoods::getStock, quantity));
        if (updated == 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "兑换库存不足");
        }
        int cost = goods.getPointsCost() * quantity;
        // 2. 原子扣积分（WHERE points>=cost）
        boolean pointsOk = userMapper.update(null,
                new LambdaUpdateWrapper<User>()
                        .setSql("points = points - " + cost)
                        .eq(User::getId, userId)
                        .ge(User::getPoints, cost)) > 0;
        if (!pointsOk) {
            // 回滚库存（原子归还）
            pointsGoodsMapper.update(null,
                    new LambdaUpdateWrapper<PointsGoods>()
                            .setSql("stock = stock + " + quantity)
                            .eq(PointsGoods::getId, goodsId));
            throw new BusinessException(ErrorCode.PARAM_ERROR, "积分不足");
        }
        // 3. 写积分流水（type=5 商城兑换）+ 兑换记录快照
        PointsRecord record = new PointsRecord();
        record.setUserId(userId);
        record.setChangeAmount(-cost);
        record.setType(5);
        record.setCreateTime(LocalDateTime.now());
        pointsRecordMapper.insert(record);
        PointsExchangeLog logEntry = new PointsExchangeLog();
        logEntry.setUserId(userId);
        logEntry.setGoodsId(goodsId);
        logEntry.setPointsCost(cost);
        logEntry.setGoodsName(goods.getName());
        logEntry.setCreateTime(LocalDateTime.now());
        pointsExchangeLogMapper.insert(logEntry);
        log.info("[积分商城] 用户={} 兑换商品={} x{} 消耗{}积分", userId, goods.getName(), quantity, cost);
    }

    /** 我的兑换记录。 */
    public List<PointsExchangeLog> listMyLogs(Long userId, int limit) {
        return pointsExchangeLogMapper.selectList(
                new LambdaQueryWrapper<PointsExchangeLog>()
                        .eq(PointsExchangeLog::getUserId, userId)
                        .orderByDesc(PointsExchangeLog::getCreateTime)
                        .last("LIMIT " + Math.min(Math.max(limit, 1), 100)));
    }

    // ==================== 管理端 ====================

    public PageResult<PointsGoods> adminList(int pageNum, int pageSize) {
        Page<PointsGoods> page = new Page<>(pageNum, pageSize);
        pointsGoodsMapper.selectPage(page,
                new LambdaQueryWrapper<PointsGoods>()
                        .eq(PointsGoods::getIsDeleted, 0)
                        .orderByDesc(PointsGoods::getCreateTime));
        return new PageResult<>(page.getTotal(), pageNum, pageSize,
                (long) Math.ceil((double) page.getTotal() / pageSize), page.getRecords());
    }

    @Transactional(rollbackFor = Exception.class)
    public void create(PointsGoods goods) {
        validateGoods(goods);
        goods.setIsDeleted(0);
        if (goods.getStatus() == null) {
            goods.setStatus(1);
        }
        pointsGoodsMapper.insert(goods);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(PointsGoods goods) {
        if (goods.getId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "商品ID不能为空");
        }
        PointsGoods existing = pointsGoodsMapper.selectById(goods.getId());
        if (existing == null || Integer.valueOf(1).equals(existing.getIsDeleted())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "兑换商品不存在");
        }
        validateGoods(goods);
        pointsGoodsMapper.updateById(goods);
    }

    /** 软删除。 */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long goodsId) {
        pointsGoodsMapper.update(null,
                new LambdaUpdateWrapper<PointsGoods>()
                        .set(PointsGoods::getIsDeleted, 1)
                        .eq(PointsGoods::getId, goodsId));
    }

    private void validateGoods(PointsGoods g) {
        if (g.getName() == null || g.getName().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "商品名称不能为空");
        }
        if (g.getName().length() > 100) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "商品名称最长100字符");
        }
        if (g.getPointsCost() == null || g.getPointsCost() <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "所需积分必须大于0");
        }
        if (g.getStock() == null || g.getStock() < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "库存不能为负");
        }
    }
}
