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
        Long userId = LoginUserContext.getCurrentUserId();
        return Result.success(recommendService.guessYouLike(userId, num));
    }

    @Operation(summary = "相似商品（公开）")
    @GetMapping("/similar/{productId}")
    public Result<List<RecommendVO>> similar(@PathVariable Long productId,
                                            @RequestParam(defaultValue = "10") Integer num) {
        return Result.success(recommendService.similar(productId, num));
    }
}
