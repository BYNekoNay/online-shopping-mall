package com.pzhu.mall.modules.logistics.controller;

import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.modules.logistics.entity.FreightTemplate;
import com.pzhu.mall.modules.logistics.service.FreightService;
import com.pzhu.mall.modules.logistics.service.LogisticsQueryService;
import com.pzhu.mall.modules.shop.service.ShopService;
import com.pzhu.mall.security.LoginUserContext;
import com.pzhu.mall.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;

/**
 * 运费模板控制器（商家端 + 消费者计算 + 物流查询）。
 */
@Tag(name = "运费与物流")
@RestController
@RequestMapping("/api")
public class FreightTemplateController {

    @Resource
    private FreightService freightService;

    @Resource
    private ShopService shopService;

    @Resource
    private LogisticsQueryService logisticsQueryService;

    @Resource
    private com.pzhu.mall.modules.order.mapper.OrderMapper orderMapper;

    // ==================== 商家端 ====================

    @Operation(summary = "运费模板列表（商家）")
    @RequireRole(2)
    @GetMapping("/merchant/freight-templates")
    public Result<List<FreightTemplate>> merchantList() {
        Long shopId = getShopId();
        return Result.success(freightService.listByShop(shopId));
    }

    @Operation(summary = "保存运费模板（商家）")
    @RequireRole(2)
    @PostMapping("/merchant/freight-templates")
    public Result<Void> merchantSave(@RequestBody FreightTemplateDTO dto) {
        Long shopId = getShopId();
        // H-05 修复：使用 DTO 接收并强制绑定当前店铺，防止传入他人模板 ID 覆盖其运费模板（IDOR）
        FreightTemplate template = new FreightTemplate();
        template.setName(dto.getName());
        template.setRegionRuleJson(dto.getRegionRuleJson());
        template.setFreeShippingThreshold(dto.getFreeShippingThreshold());
        template.setDefaultFee(dto.getDefaultFee());
        template.setShopId(shopId);
        template.setIsDeleted(0);
        if (dto.getId() != null) {
            FreightTemplate existing = freightService.getById(dto.getId());
            if (existing == null || !shopId.equals(existing.getShopId())) {
                throw new BusinessException(ErrorCode.NOT_FOUND);
            }
            template.setId(dto.getId());
        }
        freightService.save(template);
        return Result.success();
    }

    @Operation(summary = "计算运费")
    @GetMapping("/merchant/freight-templates/calculate")
    public Result<BigDecimal> calculate(@RequestParam Long shopId,
                                        @RequestParam String province,
                                        @RequestParam BigDecimal goodsAmount) {
        return Result.success(freightService.calculate(shopId, province, goodsAmount));
    }

    // ==================== 物流查询 ====================

    @Operation(summary = "查询物流轨迹")
    @GetMapping("/logistics/{orderId}/track")
    public Result<String> track(@PathVariable Long orderId) {
        // L2-04 修复：校验订单归属——消费者只能查自己的订单，商家只能查本店铺订单
        Long currentUserId = LoginUserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        com.pzhu.mall.modules.order.entity.Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        Integer role = LoginUserContext.getCurrentRole();
        boolean isConsumer = role == null || role == 1;
        if (isConsumer) {
            if (order.getUserId() == null || !order.getUserId().equals(currentUserId)) {
                throw new BusinessException(ErrorCode.NOT_FOUND);
            }
        } else {
            // 商家/管理员：校验店铺归属
            Long shopId = getShopIdQuietly(currentUserId);
            if (shopId != null && (order.getShopId() == null || !order.getShopId().equals(shopId))) {
                throw new BusinessException(ErrorCode.NOT_FOUND);
            }
        }
        try {
            String result = logisticsQueryService.query(orderId);
            return Result.success(result);
        } catch (Exception e) {
            // 第三方查询失败时降级返回提示，不抛出异常中断请求
            return Result.success("{\"status\":\"物流信息暂不可用\",\"tracks\":[]}");
        }
    }

    // ==================== 工具方法 ====================

    private Long getShopId() {
        Long userId = LoginUserContext.getCurrentUserId();
        return shopService.getMerchantShopIdOrThrow(userId);
    }

    /** 静默获取店铺 ID（非商家返回 null，用于商家/管理员物流归属校验） */
    private Long getShopIdQuietly(Long userId) {
        try {
            return shopService.getMerchantShopIdOrThrow(userId);
        } catch (Exception e) {
            return null;
        }
    }

    /** 运费模板编辑 DTO（仅包含允许客户端控制的字段）。 */
    public static class FreightTemplateDTO {
        private Long id;
        private String name;
        private String regionRuleJson;
        private java.math.BigDecimal freeShippingThreshold;
        private java.math.BigDecimal defaultFee;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getRegionRuleJson() { return regionRuleJson; }
        public void setRegionRuleJson(String regionRuleJson) { this.regionRuleJson = regionRuleJson; }
        public java.math.BigDecimal getFreeShippingThreshold() { return freeShippingThreshold; }
        public void setFreeShippingThreshold(java.math.BigDecimal freeShippingThreshold) { this.freeShippingThreshold = freeShippingThreshold; }
        public java.math.BigDecimal getDefaultFee() { return defaultFee; }
        public void setDefaultFee(java.math.BigDecimal defaultFee) { this.defaultFee = defaultFee; }
    }
}
