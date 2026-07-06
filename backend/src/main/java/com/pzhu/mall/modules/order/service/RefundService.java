package com.pzhu.mall.modules.order.service;

import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.modules.order.entity.Refund;
import com.pzhu.mall.modules.order.mapper.RefundMapper;
import com.pzhu.mall.modules.order.dto.RefundApplyDTO;
import com.pzhu.mall.modules.order.vo.RefundVO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 退款/退货服务。
 */
@Service
public class RefundService {

    @Resource
    private RefundMapper refundMapper;

    /**
     * 申请退款。
     */
    public void apply(RefundApplyDTO dto) {
        Long userId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();
        Refund refund = new Refund();
        refund.setOrderId(dto.getOrderId());
        refund.setOrderItemId(dto.getOrderItemId());
        refund.setType(dto.getType());
        refund.setReason(dto.getReason());
        refund.setAmount(dto.getAmount());
        refund.setStatus(0);
        refundMapper.insert(refund);
    }

    /**
     * 商家审核退款。
     */
    public void audit(Long refundId, Boolean approved, String handleRemark) {
        Refund refund = refundMapper.selectById(refundId);
        if (refund == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        refund.setStatus(approved ? 1 : 2);
        refund.setHandleRemark(handleRemark);
        refundMapper.updateById(refund);
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

    private RefundVO toVO(Refund r) {
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
