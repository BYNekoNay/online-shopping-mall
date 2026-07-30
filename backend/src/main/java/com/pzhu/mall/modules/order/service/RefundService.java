package com.pzhu.mall.modules.order.service;

import com.pzhu.mall.common.config.RedisKeyPrefix;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.modules.marketing.service.PointsService;
import com.pzhu.mall.modules.order.dto.RefundApplyDTO;
import com.pzhu.mall.modules.order.entity.Order;
import com.pzhu.mall.modules.order.entity.OrderItem;
import com.pzhu.mall.modules.order.entity.Refund;
import com.pzhu.mall.modules.order.mapper.OrderMapper;
import com.pzhu.mall.modules.order.mapper.OrderItemMapper;
import com.pzhu.mall.modules.order.mapper.RefundMapper;
import com.pzhu.mall.modules.order.vo.RefundVO;
import com.pzhu.mall.security.LoginUserContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 退款/退货服务。
 */
@Service
public class RefundService {

    @Resource
    private RefundMapper refundMapper;

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private OrderItemMapper orderItemMapper;

    @Resource
    private PointsService pointsService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /** 退款幂等 key 过期时间（3 天） */
    private static final long REFUND_IDEMPOTENT_TTL_HOURS = 72;

    /**
     * 申请退款（幂等：同一订单 + 同一 orderItemId 不允许重复提交）。
     * <p>
     * 校验规则：
     * <ul>
     *   <li>H-7 修复：订单状态必须为待发货/已发货/已收货/已完成，其余状态不允许退款。</li>
     *   <li>H-8 修复：同一订单存在进行中或已完成的退款记录时拒绝，防止重复退款。</li>
     *   <li>退款金额不能超过订单实付金额。</li>
     *   <li>赠品行（is_gift=1）不允许单独退款。</li>
     * </ul>
     */
    @Transactional(rollbackFor = Exception.class)
    public void apply(RefundApplyDTO dto) {
        // 0. 幂等校验（Redis SET NX）
        String idempotentKey = RedisKeyPrefix.ORDER + ":refund:apply:" + dto.getOrderId() + ":" + (dto.getOrderItemId() != null ? dto.getOrderItemId() : "all");
        Boolean alreadyApplied = stringRedisTemplate.opsForValue().setIfAbsent(idempotentKey, "1", REFUND_IDEMPOTENT_TTL_HOURS, TimeUnit.HOURS);
        if (alreadyApplied != null && !alreadyApplied) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "已提交过退款申请，请勿重复操作");
        }

        // 1. 校验退款金额不超过订单实付金额
        Order order = orderMapper.selectById(dto.getOrderId());
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        // H-01 修复：校验订单归属当前用户，防止 IDOR（任意用户可对他人订单发起退款）
        Long currentUserId = LoginUserContext.getCurrentUserId();
        if (order.getUserId() == null || !order.getUserId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        // H-7 修复：仅"待发货(1)/已发货(2)/已收货(3)/已完成(4)"允许申请退款。
        // 待付款(0)/已取消(5)订单没有支付事实，对其退款会造成非法状态迁移（如 5→7）
        int status = order.getStatus() != null ? order.getStatus() : -1;
        if (status != 1 && status != 2 && status != 3 && status != 4) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        // H-8 修复：同一订单已存在"待审核(0)/已通过(1)"的退款记录时拒绝再次申请。
        // 积分扣回按订单维度全额执行，多笔退款会导致重复扣回；
        // Redis 幂等键按 orderItemId 区分且 72h 过期，此处按 orderId 维度兜底
        Long existing = refundMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Refund>()
                        .eq(Refund::getOrderId, dto.getOrderId())
                        .in(Refund::getStatus, 0, 1)
        );
        if (existing != null && existing > 0) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "该订单已有退款记录，请勿重复申请");
        }
        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "退款金额必须大于 0");
        }
        if (dto.getAmount().compareTo(order.getPayAmount()) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "退款金额不能超过订单实付金额");
        }

        // 2. 赠品行排除：若指定了 orderItemId，检查是否为赠品
        if (dto.getOrderItemId() != null) {
            OrderItem item = orderItemMapper.selectById(dto.getOrderItemId());
            if (item == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND);
            }
            // M-04 修复：校验订单行归属于当前订单，防止引用他人订单的行
            if (item.getOrderId() == null || !item.getOrderId().equals(dto.getOrderId())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "订单行不属于该订单");
            }
            if (item.getIsGift() != null && item.getIsGift() == 1) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "赠品不允许单独退款");
            }
        }

        // 3. 创建退款记录
        Refund refund = new Refund();
        refund.setOrderId(dto.getOrderId());
        refund.setOrderItemId(dto.getOrderItemId());
        refund.setType(dto.getType());
        refund.setReason(dto.getReason());
        refund.setAmount(dto.getAmount());
        refund.setStatus(0); // 待审核
        refundMapper.insert(refund);
    }

    /**
     * 商家审核退款（校验店铺归属）。
     * <p>
     * 审核通过时：
     * <ul>
     *   <li>更新退款状态为已通过。</li>
     *   <li>同步更新订单状态为"已退款"（status=7）。</li>
     *   <li>调用 PointsService.clawback 扣回该订单产生的积分。</li>
     *   <li>H-5 修复：调用 PointsService.refundDeduct 返还下单时抵扣的积分。</li>
     * </ul>
     *
     * @param shopId 商家店铺 ID，用于校验退款订单归属
     */
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long refundId, Boolean approved, String handleRemark, Long shopId) {
        Refund refund = refundMapper.selectById(refundId);
        if (refund == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (refund.getStatus() != 0) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }

        // 校验订单店铺归属
        Order order = orderMapper.selectById(refund.getOrderId());
        if (order == null || !order.getShopId().equals(shopId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        // M7 修复：原子更新，WHERE status=0 防重复审核
        int newStatus = Boolean.TRUE.equals(approved) ? 1 : 2;
        boolean updated = refundMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Refund>()
                        .set(Refund::getStatus, newStatus)
                        .set(Refund::getHandleRemark, handleRemark)
                        .eq(Refund::getId, refundId)
                        .eq(Refund::getStatus, 0)
        ) > 0;
        if (!updated) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }

        if (approved) {
            // 同步更新订单状态为"已退款"（原子更新，WHERE status != 7 防重复）
            if (order.getStatus() != 7) {
                orderMapper.update(null,
                        new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Order>()
                                .set(Order::getStatus, 7)
                                .eq(Order::getId, order.getId())
                                .ne(Order::getStatus, 7)
                );
            }
            // 扣回积分
            pointsService.clawback(refund.getOrderId());
            // H-5 修复：返还下单时抵扣的积分（支付已撤销，作为支付手段的积分应退回；方法自带幂等守卫）
            pointsService.refundDeduct(refund.getOrderId());
        }
    }

    /**
     * 获取当前用户的退款列表。
     * <p>通过当前用户的订单 ID 过滤退款记录，避免用户看到他人退款信息。</p>
     */
    public List<RefundVO> listByUser() {
        Long userId = LoginUserContext.getCurrentUserId();
        // 先查出当前用户的所有订单 ID
        List<Long> userOrderIds = orderMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order>()
                .select(Order::getId)
                .eq(Order::getUserId, userId)
        ).stream().map(Order::getId).collect(java.util.stream.Collectors.toList());
        if (userOrderIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        List<Refund> list = refundMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Refund>()
                .in(Refund::getOrderId, userOrderIds)
                .orderByDesc(Refund::getCreateTime)
        );
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    public RefundVO toVO(Refund r) {
        RefundVO vo = new RefundVO();
        vo.setId(r.getId());
        vo.setOrderId(r.getOrderId());
        vo.setOrderItemId(r.getOrderItemId());
        vo.setType(r.getType());
        vo.setReason(r.getReason());
        vo.setAmount(r.getAmount());
        vo.setStatus(r.getStatus());
        vo.setHandleRemark(r.getHandleRemark());
        vo.setCreateTime(r.getCreateTime());
        return vo;
    }
}
