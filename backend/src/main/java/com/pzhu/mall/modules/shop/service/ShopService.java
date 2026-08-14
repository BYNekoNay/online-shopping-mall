package com.pzhu.mall.modules.shop.service;

import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.modules.shop.entity.Shop;
import com.pzhu.mall.modules.shop.mapper.ShopMapper;
import com.pzhu.mall.modules.user.entity.User;
import com.pzhu.mall.modules.user.mapper.UserMapper;
import com.pzhu.mall.modules.shop.dto.ShopApplyDTO;
import com.pzhu.mall.modules.shop.dto.ShopUpdateDTO;
import com.pzhu.mall.modules.shop.vo.ShopApplyStatusVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ShopService {

    private final ShopMapper shopMapper;
    private final UserMapper userMapper;

    public ShopService(ShopMapper shopMapper, UserMapper userMapper) {
        this.shopMapper = shopMapper;
        this.userMapper = userMapper;
    }

    /**
     * 商家入驻申请。
     */
    @Transactional
    public ShopApplyStatusVO apply(Long merchantUserId, ShopApplyDTO dto) {
        // SH-02 修复：空请求体防护（controller @Validated 仅校验非 null 请求体）
        if (dto == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "入驻申请信息不能为空");
        }
        // 查询是否已有申请记录
        var qw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Shop>();
        qw.eq(Shop::getMerchantUserId, merchantUserId).eq(Shop::getIsDeleted, 0);
        Shop exist = shopMapper.selectOne(qw);

        if (exist != null) {
            // 已有且状态为 待审核/正常/已禁用，不允许重复申请
            if (exist.getStatus() == 0 || exist.getStatus() == 1 || exist.getStatus() == 3) {
                throw new BusinessException(ErrorCode.PARAM_ERROR);
            }
            // status == 2（已拒绝），允许重新提交，复用同一条记录
            exist.setName(dto.getName());
            exist.setContactName(dto.getContactName());
            exist.setContactPhone(dto.getContactPhone());
            exist.setLicenseNo(dto.getLicenseNo());
            exist.setLicenseImage(dto.getLicenseImage());
            exist.setApplyReason(dto.getApplyReason());
            exist.setStatus(0);
            exist.setRejectReason(null);
            exist.setUpdateTime(LocalDateTime.now());
            shopMapper.updateById(exist);
            return toStatusVO(exist);
        }

        // 无记录，新增
        Shop shop = new Shop();
        shop.setMerchantUserId(merchantUserId);
        shop.setName(dto.getName());
        shop.setContactName(dto.getContactName());
        shop.setContactPhone(dto.getContactPhone());
        shop.setLicenseNo(dto.getLicenseNo());
        shop.setLicenseImage(dto.getLicenseImage());
        shop.setApplyReason(dto.getApplyReason());
        shop.setStatus(0);
        shop.setLevel(1);
        shop.setIsDeleted(0);
        shopMapper.insert(shop);
        return toStatusVO(shop);
    }

    /**
     * 查询入驻申请状态。
     */
    public ShopApplyStatusVO applyStatus(Long merchantUserId) {
        var qw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Shop>();
        qw.eq(Shop::getMerchantUserId, merchantUserId).eq(Shop::getIsDeleted, 0);
        Shop shop = shopMapper.selectOne(qw);
        if (shop == null) {
            return new ShopApplyStatusVO();
        }
        return toStatusVO(shop);
    }

    /**
     * 获取当前商家的店铺信息。
     */
    public Shop getMerchantShop(Long merchantUserId) {
        var qw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Shop>();
        qw.eq(Shop::getMerchantUserId, merchantUserId).eq(Shop::getStatus, 1).eq(Shop::getIsDeleted, 0);
        Shop shop = shopMapper.selectOne(qw);
        if (shop == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return shop;
    }

    /**
     * 更新店铺信息（仅允许更新 name/logo/description/decorationConfig）。
     *
     * <p>M-18 修复：逐字段 null 守卫，仅更新前端实际提交的字段，
     * 避免部分更新时 null 值污染实体（配合 updateById 的 NOT_NULL 策略形成双重保护）。
     */
    @Transactional
    public void updateInfo(Long merchantUserId, ShopUpdateDTO dto) {
        Shop shop = getMerchantShop(merchantUserId); // 校验 status=1
        if (dto.getName() != null) {
            shop.setName(dto.getName());
        }
        if (dto.getLogo() != null) {
            shop.setLogo(dto.getLogo());
        }
        if (dto.getDescription() != null) {
            shop.setDescription(dto.getDescription());
        }
        if (dto.getDecorationConfig() != null) {
            shop.setDecorationConfig(dto.getDecorationConfig());
        }
        shop.setUpdateTime(LocalDateTime.now());
        shopMapper.updateById(shop);
    }

    /**
     * 管理员审核店铺。
     *
     * <p>AD-01 修复：审核通过（approved=true）时，在同一事务内将店铺所属用户的角色
     * 升级为 2（商家），使商家端 /api/merchant/* 与前端 /merchant 路由立即可用，
     * 消除"入驻成功但商家端 403"的业务断裂。
     */
    @Transactional
    public void audit(Long shopId, boolean approved, String reason) {
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        // H-09 + M-17 修复：仅允许审核"待审核"(status=0) 状态的店铺，
        // 使用 WHERE status=0 的原子更新，防止两个管理员并发审核时后写覆盖前写
        int updated = shopMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Shop>()
                        .set(Shop::getStatus, approved ? 1 : 2)
                        .set(Shop::getRejectReason, approved ? null : reason)
                        .set(Shop::getUpdateTime, LocalDateTime.now())
                        .eq(Shop::getId, shopId)
                        .eq(Shop::getStatus, 0)
        );
        if (updated == 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "店铺不处于待审核状态，无法审核");
        }
        // AD-01 修复：审核通过 → 联动升级商家角色（role=2），与店铺状态更新同事务
        if (approved && shop.getMerchantUserId() != null) {
            userMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<User>()
                            .set(User::getRole, 2)
                            .eq(User::getId, shop.getMerchantUserId())
                            .eq(User::getIsDeleted, 0)
            );
        }
    }

    /**
     * 获取商家店铺 ID（校验 status=1），供其他 Service 调用。
     */
    public Long getMerchantShopIdOrThrow(Long merchantUserId) {
        Shop shop = getMerchantShop(merchantUserId);
        return shop.getId();
    }

    private ShopApplyStatusVO toStatusVO(Shop shop) {
        ShopApplyStatusVO vo = new ShopApplyStatusVO();
        vo.setHasApplied(true);
        vo.setShopId(shop.getId());
        vo.setStatus(shop.getStatus());
        vo.setRejectReason(shop.getRejectReason());
        return vo;
    }
}
