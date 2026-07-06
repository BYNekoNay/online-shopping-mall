package com.pzhu.mall.modules.behavior.controller;

import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.modules.behavior.dto.BehaviorRecordDTO;
import com.pzhu.mall.modules.behavior.dto.PageViewDTO;
import com.pzhu.mall.modules.behavior.dto.PageLeaveDTO;
import com.pzhu.mall.modules.behavior.service.BehaviorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 用户行为埋点控制器。
 */
@Tag(name = "用户行为")
@RestController
@RequestMapping("/api/behavior")
public class BehaviorController {

    @Resource
    private BehaviorService behaviorService;

    @Operation(summary = "记录行为（浏览/收藏/购买/评价）")
    @PostMapping("/record")
    public Result<Void> record(@RequestBody BehaviorRecordDTO dto) {
        behaviorService.record(dto.getUserId(), dto.getProductId(), dto.getBehaviorType());
        return Result.success();
    }

    @Operation(summary = "页面进入上报")
    @PostMapping("/page-view")
    public Result<Long> pageEnter(@RequestBody PageViewDTO dto) {
        return Result.success(behaviorService.recordPageEnter(dto));
    }

    @Operation(summary = "页面离开回填")
    @PutMapping("/page-view/{id}/leave")
    public Result<Void> pageLeave(@PathVariable Long id, @RequestBody PageLeaveDTO dto) {
        behaviorService.recordPageLeave(id, dto.getStayDuration());
        return Result.success();
    }
}
