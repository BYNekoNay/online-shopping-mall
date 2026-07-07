package com.pzhu.mall.modules.admin.controller;

import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.common.result.PageResult;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.modules.shop.entity.Shop;
import com.pzhu.mall.modules.shop.mapper.ShopMapper;
import com.pzhu.mall.modules.shop.service.ShopService;
import com.pzhu.mall.modules.shop.vo.ShopVO;
import com.pzhu.mall.modules.admin.service.OperationLogService;
import com.pzhu.mall.security.LoginUserContext;
import com.pzhu.mall.security.RequireRole;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 管理员端店铺审核控制器。
 */
@Tag(name = "管理员-店铺管理")
@RestController
@RequestMapping("/api/admin/shops")
@RequireRole(3)
public class AdminShopController {

    @Resource
    private ShopService shopService;

    @Resource
    private ShopMapper shopMapper;

    @Resource
    private OperationLogService operationLogService;

    @Operation(summary = "店铺列表")
    @GetMapping
    public Result<PageResult<ShopVO>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                           @RequestParam(defaultValue = "10") Integer pageSize,
                                           @RequestParam(required = false) Integer status) {
        var qw = new LambdaQueryWrapper<Shop>().eq(Shop::getIsDeleted, 0);
        if (status != null) {
            qw.eq(Shop::getStatus, status);
        }
        qw.orderByDesc(Shop::getCreateTime);

        Page<Shop> page = new Page<>(pageNum, pageSize);
        var mpPage = shopMapper.selectPage(page, qw);

        List<ShopVO> voList = mpPage.getRecords().stream()
                .map(s -> {
                    ShopVO vo = new ShopVO();
                    vo.setId(s.getId());
                    vo.setMerchantUserId(s.getMerchantUserId());
                    vo.setName(s.getName());
                    vo.setStatus(s.getStatus());
                    vo.setContactName(s.getContactName());
                    vo.setContactPhone(s.getContactPhone());
                    vo.setApplyReason(s.getApplyReason());
                    vo.setRejectReason(s.getRejectReason());
                    vo.setCreateTime(s.getCreateTime());
                    vo.setUpdateTime(s.getUpdateTime());
                    return vo;
                })
                .toList();

        return Result.success(new PageResult<>(mpPage.getTotal(), pageNum, pageSize, mpPage.getPages(), voList));
    }

    @Operation(summary = "审核店铺")
    @PutMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @RequestBody AuditDTO dto) {
        shopService.audit(id, dto.getApproved(), dto.getReason());
        Long operatorId = LoginUserContext.getCurrentUserId();
        operationLogService.record(
                operatorId,
                dto.getApproved() ? "审核店铺通过" : "审核店铺拒绝",
                "店铺#" + id
        );
        return Result.success();
    }

    @Operation(summary = "调整商家等级")
    @PutMapping("/{id}/level")
    public Result<Void> updateLevel(@PathVariable Long id, @RequestBody LevelDTO dto) {
        Shop shop = shopMapper.selectById(id);
        if (shop == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        shop.setLevel(dto.getLevel());
        shopMapper.updateById(shop);

        Long operatorId = LoginUserContext.getCurrentUserId();
        operationLogService.record(
                operatorId,
                "调整商家等级为" + dto.getLevel(),
                "店铺#" + id
        );
        return Result.success();
    }

    public static class AuditDTO {
        private Boolean approved;
        private String reason;
        public Boolean getApproved() { return approved; }
        public void setApproved(Boolean approved) { this.approved = approved; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class LevelDTO {
        private Integer level;
        public Integer getLevel() { return level; }
        public void setLevel(Integer level) { this.level = level; }
    }
}
