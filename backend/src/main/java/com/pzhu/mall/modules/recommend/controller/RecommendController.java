package com.pzhu.mall.modules.recommend.controller;

import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.modules.recommend.service.RecommendService;
import com.pzhu.mall.modules.recommend.vo.RecommendVO;
import com.pzhu.mall.security.LoginUserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 推荐控制器。
 * <p>猜你喜欢接口为公开可选登录：携带合法 Token 返回个性化推荐，不携带或未登录返回热门兜底。</p>
 */
@Tag(name = "推荐")
@RestController
@RequestMapping("/api/recommend")
public class RecommendController {

    @Resource
    private RecommendService recommendService;

    @Operation(summary = "猜你喜欢（公开，可选登录）")
    @GetMapping("/guess-you-like")
    public Result<List<RecommendVO>> guessYouLike(@RequestParam(defaultValue = "10") Integer num) {
        // R-08 修复：num 钳制到 [1,50]，防超大值大查询（DoS 面）
        num = clampNum(num);
        Long userId = LoginUserContext.getCurrentUserId();
        return Result.success(recommendService.guessYouLike(userId, num));
    }

    @Operation(summary = "相似商品（公开）")
    @GetMapping("/similar/{productId}")
    public Result<List<RecommendVO>> similar(@PathVariable Long productId,
                                            @RequestParam(defaultValue = "10") Integer num) {
        // R-08 修复：num 钳制到 [1,50]
        num = clampNum(num);
        return Result.success(recommendService.similar(productId, num));
    }

    @Operation(summary = "浏览历史推荐（需登录，A-1）")
    @GetMapping("/history")
    public Result<List<RecommendVO>> history(@RequestParam(defaultValue = "10") Integer num) {
        // R-08 修复：num 钳制到 [1,50]
        num = clampNum(num);
        Long userId = LoginUserContext.getCurrentUserId();
        if (userId == null) {
            // 双保险：拦截器理论上已拦未登录；此处兜底返回空
            return Result.success(List.of());
        }
        return Result.success(recommendService.historyBased(userId, num));
    }

    @Operation(summary = "购买推荐（需登录，D-5）")
    @GetMapping("/purchase")
    public Result<List<RecommendVO>> purchase(@RequestParam(defaultValue = "10") Integer num) {
        num = clampNum(num);
        Long userId = LoginUserContext.getCurrentUserId();
        if (userId == null) {
            return Result.success(List.of());
        }
        return Result.success(recommendService.purchaseBased(userId, num));
    }

    /** R-08 修复：推荐数量参数钳制，非法/超限回退默认值 10 */
    private static int clampNum(Integer num) {
        if (num == null || num <= 0 || num > 50) {
            return 10;
        }
        return num;
    }
}
