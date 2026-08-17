package com.pzhu.mall.modules.behavior.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.modules.behavior.dto.BehaviorRecordDTO;
import com.pzhu.mall.modules.behavior.dto.PageViewDTO;
import com.pzhu.mall.modules.behavior.dto.PageLeaveDTO;
import com.pzhu.mall.modules.behavior.entity.UserBehavior;
import com.pzhu.mall.modules.behavior.mapper.UserBehaviorMapper;
import com.pzhu.mall.modules.behavior.service.BehaviorService;
import com.pzhu.mall.modules.product.entity.Product;
import com.pzhu.mall.modules.product.mapper.ProductMapper;
import com.pzhu.mall.modules.behavior.dto.RecommendExposureDTO;
import com.pzhu.mall.modules.behavior.dto.RecommendClickDTO;
import com.pzhu.mall.security.LoginUserContext;
import com.pzhu.mall.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 用户行为埋点控制器。
 */
@Tag(name = "用户行为")
@RestController
@RequestMapping("/api/behavior")
public class BehaviorController {

    @Resource
    private BehaviorService behaviorService;

    @Resource
    private UserBehaviorMapper userBehaviorMapper;

    @Resource
    private ProductMapper productMapper;

    @Operation(summary = "记录行为（浏览/收藏/购买/评价）")
    @PostMapping("/record")
    public Result<Void> record(@RequestBody BehaviorRecordDTO dto) {
        // M-11 修复：userId 只取登录态，不信任前端传入的 userId，防止伪造他人 userId 污染行为数据；
        // 未登录时不记录（user_behavior.user_id 为 NOT NULL，匿名行为不入库），直接返回成功
        Long userId = LoginUserContext.getCurrentUserId();
        if (userId == null) {
            return Result.success();
        }
        behaviorService.record(userId, dto.getProductId(), dto.getBehaviorType());
        return Result.success();
    }

    @Operation(summary = "推荐位曝光埋点")
    @PostMapping("/recommend-exposure")
    public Result<Void> recommendExposure(@RequestBody RecommendExposureDTO dto) {
        // FRONT-01 修复：推荐位埋点匿名放行——未登录时不落库、静默返回成功（参考 /behavior/record 模式），
        // 避免匿名用户访问首页时因 10002 被前端拦截器强制跳转登录页
        Long userId = LoginUserContext.getCurrentUserId();
        if (userId == null) {
            return Result.success();
        }
        // 埋点为 best-effort：source/productIds 缺失时静默忽略，不阻塞页面
        if (dto == null || dto.getSource() == null || dto.getProductIds() == null || dto.getProductIds().isEmpty()) {
            return Result.success();
        }
        behaviorService.recordRecommendExposure(dto);
        return Result.success();
    }

    @Operation(summary = "推荐位点击埋点（记为浏览行为 type=1）")
    @PostMapping("/recommend-click")
    public Result<Void> recommendClick(@RequestBody RecommendClickDTO dto) {
        // FRONT-01 修复：匿名放行——未登录不记录点击，静默返回成功，避免匿名用户被强制跳转登录
        Long userId = LoginUserContext.getCurrentUserId();
        if (userId == null) {
            return Result.success();
        }
        // M-11 修复：userId 强制取登录态，不信任 DTO 传入值，防止已登录用户伪造他人点击污染推荐输入
        if (dto == null || dto.getProductId() == null) {
            return Result.success();
        }
        dto.setUserId(userId);
        behaviorService.recordRecommendClick(dto);
        return Result.success();
    }

    @Operation(summary = "页面进入上报")
    @PostMapping("/page-view")
    public Result<Long> pageEnter(@Valid @RequestBody PageViewDTO dto) {
        return Result.success(behaviorService.recordPageEnter(dto));
    }

    @Operation(summary = "页面离开回填")
    @PutMapping("/page-view/{id}/leave")
    public Result<Void> pageLeave(@PathVariable Long id, @Valid @RequestBody PageLeaveDTO dto) {
        behaviorService.recordPageLeave(id, dto.getStayDuration());
        return Result.success();
    }

    @Operation(summary = "我的收藏列表")
    @RequireRole(1)
    @GetMapping("/favorites")
    public Result<List<com.pzhu.mall.modules.behavior.vo.FavoriteVO>> favorites() {
        Long userId = LoginUserContext.getCurrentUserId();
        LambdaQueryWrapper<UserBehavior> qw = new LambdaQueryWrapper<>();
        qw.eq(UserBehavior::getUserId, userId)
          .eq(UserBehavior::getBehaviorType, 2) // 2=收藏
          .orderByDesc(UserBehavior::getCreateTime);
        List<UserBehavior> behaviors = userBehaviorMapper.selectList(qw);

        if (behaviors.isEmpty()) {
            return Result.success(java.util.Collections.emptyList());
        }

        // 批量加载商品信息（消除 N+1 查询）
        List<Long> productIds = behaviors.stream()
                .map(UserBehavior::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<Product> products = productMapper.selectBatchIds(productIds);
        // 按收藏顺序返回，过滤已删除商品（F-04 修复：返回轻量 FavoriteVO，避免 Product 大字段透出）
        Map<Long, Product> productMap = products.stream()
                .filter(p -> p.getIsDeleted() == 0)
                .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));
        List<com.pzhu.mall.modules.behavior.vo.FavoriteVO> ordered = behaviors.stream()
                .map(b -> productMap.get(b.getProductId()))
                .filter(Objects::nonNull)
                .map(com.pzhu.mall.modules.behavior.vo.FavoriteVO::from)
                .collect(Collectors.toList());
        return Result.success(ordered);
    }

    @Operation(summary = "收藏商品")
    @RequireRole(1)
    @PostMapping("/favorites/{productId}")
    public Result<Void> favorite(@PathVariable Long productId) {
        Long userId = LoginUserContext.getCurrentUserId();
        behaviorService.record(userId, productId, 2); // 2=收藏
        return Result.success();
    }

    @Operation(summary = "取消收藏")
    @RequireRole(1)
    @DeleteMapping("/favorites/{productId}")
    public Result<Void> unfavorite(@PathVariable Long productId) {
        Long userId = LoginUserContext.getCurrentUserId();
        LambdaQueryWrapper<UserBehavior> qw = new LambdaQueryWrapper<>();
        qw.eq(UserBehavior::getUserId, userId)
          .eq(UserBehavior::getProductId, productId)
          .eq(UserBehavior::getBehaviorType, 2);
        userBehaviorMapper.delete(qw);
        return Result.success();
    }
}
