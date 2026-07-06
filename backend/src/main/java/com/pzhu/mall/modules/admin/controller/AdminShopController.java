package com.pzhu.mall.modules.admin.controller;

import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.modules.shop.service.ShopService;
import com.pzhu.mall.modules.shop.vo.ShopVO;
import com.pzhu.mall.security.RequireRole;
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

    @Operation(summary = "店铺列表")
    @GetMapping
    public Result<List<ShopVO>> list() {
        // TODO: 实现分页查询
        throw new BusinessException(ErrorCode.NOT_FOUND);
    }

    @Operation(summary = "审核店铺")
    @PutMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @RequestBody AuditDTO dto) {
        shopService.audit(id, dto.getApproved(), dto.getReason());
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
}
