package com.pzhu.mall.modules.shop.controller;

import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.modules.shop.dto.ShopApplyDTO;
import com.pzhu.mall.modules.shop.dto.ShopUpdateDTO;
import com.pzhu.mall.modules.shop.entity.Shop;
import com.pzhu.mall.modules.shop.service.ShopService;
import com.pzhu.mall.modules.shop.vo.ShopApplyStatusVO;
import com.pzhu.mall.modules.shop.vo.ShopVO;
import com.pzhu.mall.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 商家端店铺控制器。
 */
@Tag(name = "商家店铺管理")
@RestController
@RequestMapping("/api/merchant/shop")
@RequireRole(2)
public class ShopController {

    @Resource
    private ShopService shopService;

    @Operation(summary = "入驻申请")
    @PostMapping("/apply")
    public Result<ShopApplyStatusVO> apply(@Validated @RequestBody ShopApplyDTO dto) {
        Long userId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();
        return Result.success(shopService.apply(userId, dto));
    }

    @Operation(summary = "查询申请状态")
    @GetMapping("/apply-status")
    public Result<ShopApplyStatusVO> applyStatus() {
        Long userId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();
        return Result.success(shopService.applyStatus(userId));
    }

    @Operation(summary = "获取店铺信息")
    @GetMapping
    public Result<ShopVO> getInfo() {
        Long userId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();
        Shop shop = shopService.getMerchantShop(userId);
        ShopVO vo = new ShopVO();
        vo.setId(shop.getId());
        vo.setMerchantUserId(shop.getMerchantUserId());
        vo.setName(shop.getName());
        vo.setLogo(shop.getLogo());
        vo.setDescription(shop.getDescription());
        vo.setDecorationConfig(shop.getDecorationConfig());
        vo.setLevel(shop.getLevel());
        vo.setStatus(shop.getStatus());
        vo.setContactName(shop.getContactName());
        vo.setContactPhone(shop.getContactPhone());
        vo.setLicenseNo(shop.getLicenseNo());
        vo.setLicenseImage(shop.getLicenseImage());
        vo.setApplyReason(shop.getApplyReason());
        vo.setRejectReason(shop.getRejectReason());
        vo.setCreateTime(shop.getCreateTime());
        vo.setUpdateTime(shop.getUpdateTime());
        return Result.success(vo);
    }

    @Operation(summary = "更新店铺信息")
    @PutMapping
    public Result<Void> updateInfo(@Validated @RequestBody ShopUpdateDTO dto) {
        Long userId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();
        shopService.updateInfo(userId, dto);
        return Result.success();
    }
}
