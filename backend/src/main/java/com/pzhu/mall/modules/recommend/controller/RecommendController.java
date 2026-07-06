package com.pzhu.mall.modules.recommend.controller;

import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.modules.product.entity.Product;
import com.pzhu.mall.modules.recommend.service.RecommendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 推荐控制器。
 */
@Tag(name = "推荐")
@RestController
@RequestMapping("/api/recommend")
public class RecommendController {

    @Resource
    private RecommendService recommendService;

    @Operation(summary = "猜你喜欢")
    @GetMapping("/guess-you-like")
    public Result<List<Product>> guessYouLike(@RequestParam(defaultValue = "10") Integer num) {
        Long userId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();
        return Result.success(recommendService.guessYouLike(userId, num));
    }

    @Operation(summary = "相似商品")
    @GetMapping("/similar/{productId}")
    public Result<List<Product>> similar(@PathVariable Long productId,
                                        @RequestParam(defaultValue = "10") Integer num) {
        return Result.success(recommendService.similar(productId, num));
    }
}
