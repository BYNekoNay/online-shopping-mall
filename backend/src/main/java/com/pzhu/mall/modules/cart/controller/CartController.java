package com.pzhu.mall.modules.cart.controller;

import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.modules.cart.entity.Cart;
import com.pzhu.mall.modules.cart.service.CartService;
import com.pzhu.mall.modules.cart.vo.CartVO;
import com.pzhu.mall.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 购物车控制器。
 */
@Tag(name = "购物车")
@RestController
@RequestMapping("/api/cart")
@RequireRole(1)
public class CartController {

    @Resource
    private CartService cartService;

    @Operation(summary = "购物车列表")
    @GetMapping
    public Result<List<CartVO>> list() {
        return Result.success(cartService.listVO());
    }

    @Operation(summary = "添加商品到购物车")
    @PostMapping
    public Result<Void> add(@RequestBody Cart cart) {
        cartService.add(cart);
        return Result.success();
    }

    @Operation(summary = "更新购物车项数量/选中状态")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Cart cart) {
        cartService.update(id, cart);
        return Result.success();
    }

    @Operation(summary = "删除购物车项")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        cartService.delete(id);
        return Result.success();
    }
}
