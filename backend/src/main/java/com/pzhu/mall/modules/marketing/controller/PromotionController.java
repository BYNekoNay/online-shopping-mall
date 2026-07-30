package com.pzhu.mall.modules.marketing.controller;

import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.modules.marketing.entity.Promotion;
import com.pzhu.mall.modules.marketing.mapper.PromotionMapper;
import com.pzhu.mall.modules.marketing.service.PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 促销活动消费者端控制器。
 */
@Tag(name = "促销活动（消费者）")
@RestController
@RequestMapping("/api/promotions")
public class PromotionController {

    @Resource
    private PromotionService promotionService;

    @Resource
    private PromotionMapper promotionMapper;

    @Operation(summary = "获取当前生效的促销活动列表（消费者）")
    @GetMapping("/active")
    public Result<List<Promotion>> active(
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) Long scopeId) {
        List<Promotion> promotions;
        if (scope != null && scopeId != null) {
            promotions = promotionMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Promotion>()
                            .eq(Promotion::getStatus, 1)
                            .eq(Promotion::getIsDeleted, 0)
                            .eq(Promotion::getScope, scope)
                            .eq(Promotion::getScopeId, scopeId)
                            // H-13 修复：时间条件写反导致长期促销对消费者不可见，
                            // 正确语义为"当前生效"：开始时间 <= now <= 结束时间
                            .le(Promotion::getStartTime, LocalDateTime.now())
                            .ge(Promotion::getEndTime, LocalDateTime.now())
            );
        } else {
            promotions = promotionService.listActive();
        }
        return Result.success(promotions);
    }

}
