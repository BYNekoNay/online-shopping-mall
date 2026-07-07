package com.pzhu.mall.modules.marketing.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.modules.user.entity.User;
import com.pzhu.mall.modules.user.mapper.UserMapper;
import com.pzhu.mall.modules.marketing.entity.PointsRecord;
import com.pzhu.mall.modules.marketing.mapper.PointsRecordMapper;
import com.pzhu.mall.modules.marketing.service.PointsService;
import com.pzhu.mall.security.LoginUserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 积分控制器。
 */
@Tag(name = "积分")
@RestController
@RequestMapping("/api/user/points")
public class PointsController {

    @Resource
    private UserMapper userMapper;

    @Resource
    private PointsRecordMapper pointsRecordMapper;

    @Resource
    private PointsService pointsService;

    @Operation(summary = "我的积分余额")
    @GetMapping
    public Result<Map<String, Object>> balance() {
        Long userId = LoginUserContext.getCurrentUserId();
        User user = userMapper.selectById(userId);
        int points = user != null && user.getPoints() != null ? user.getPoints() : 0;
        return Result.success(Map.of("points", points));
    }

    @Operation(summary = "积分变动流水")
    @GetMapping("/records")
    public Result<Map<String, Object>> records(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Long userId = LoginUserContext.getCurrentUserId();
        long total = pointsService.countRecords(userId);
        List<PointsRecord> records = pointsService.listRecords(userId, pageNum, pageSize);
        return Result.success(Map.of(
                "records", records,
                "total", total,
                "pageNum", pageNum,
                "pageSize", pageSize
        ));
    }
}
