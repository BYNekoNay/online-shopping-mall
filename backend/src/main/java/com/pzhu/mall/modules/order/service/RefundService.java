package com.pzhu.mall.modules.order.service;

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
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
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

    /**
     * 申请退款。
     * <p>
     * 校验规则：
     * <ul>
     *   <li>退款金额不能超过订单实付金额。</li>
     *   <li>赠品行（is_gift=1）不允许单独退款。</li>
     * </ul>
     */
    public void apply(RefundApplyDTO dto) {
        // 1. 校验退款金额不超过订单实付金额
        Order order = orderMapper.selectById(dto.getOrderId());
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
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
     * 商家审核退款。
     * <p>
     * 审核通过时：
     * <ul>
     *   <li>更新退款状态为已通过。</li>
     *   <li>调用 PointsService.clawback 扣回该订单产生的积分。</li>
     * </ul>
     */
    public void audit(Long refundId, Boolean approved, String handleRemark) {
        Refund refund = refundMapper.selectById(refundId);
        if (refund == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        refund.setStatus(approved ? 1 : 2);
        refund.setHandleRemark(handleRemark);
        refundMapper.updateById(refund);

        // 审核通过：扣回积分
        if (approved) {
            pointsService.clawback(refund.getOrderId());
        }
    }

    /**
     * 获取当前用户的退款列表。
     */
    public List<RefundVO> listByUser() {
        List<Refund> list = refundMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Refund>()
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
