package com.pzhu.mall.modules.shop.controller;

import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.modules.shop.dto.ShopApplyDTO;
import com.pzhu.mall.modules.shop.dto.ShopUpdateDTO;
import com.pzhu.mall.modules.shop.service.ShopService;
import com.pzhu.mall.modules.shop.vo.ShopApplyStatusVO;
import com.pzhu.mall.modules.shop.vo.ShopVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 商家端店铺控制器。
 */
@Tag(name = "商家店铺管理")
@RestController
@RequestMapping("/api/merchant/shop")
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
    public Result<com.pzhu.mall.modules.shop.entity.Shop> getInfo() {
        Long userId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();
        return Result.success(shopService.getMerchantShop(userId));
    }

    @Operation(summary = "更新店铺信息")
    @PutMapping
    public Result<Void> updateInfo(@Validated @RequestBody ShopUpdateDTO dto) {
        Long userId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();
        shopService.updateInfo(userId, dto);
        return Result.success();
    }
}
