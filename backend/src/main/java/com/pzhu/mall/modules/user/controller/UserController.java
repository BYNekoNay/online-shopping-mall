package com.pzhu.mall.modules.user.controller;

import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.modules.user.dto.LoginDTO;
import com.pzhu.mall.modules.user.dto.RegisterDTO;
import com.pzhu.mall.modules.user.dto.UpdateProfileDTO;
import com.pzhu.mall.modules.user.entity.Address;
import com.pzhu.mall.modules.user.entity.User;
import com.pzhu.mall.modules.user.service.AddressService;
import com.pzhu.mall.modules.user.service.UserService;
import com.pzhu.mall.modules.user.vo.LoginVO;
import com.pzhu.mall.modules.user.vo.UserProfileVO;
import com.pzhu.mall.security.LoginUserContext;
import com.pzhu.mall.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 用户与鉴权控制器。
 * 注册/登录为公开端点（白名单放行），其余需登录。
 */
@Tag(name = "用户与鉴权")
@RestController
@RequestMapping("/api")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private AddressService addressService;

    @Operation(summary = "用户注册")
    @PostMapping("/auth/register")
    public Result<Void> register(@Validated @RequestBody RegisterDTO dto) {
        userService.register(dto.getUsername(), dto.getPassword(), dto.getNickname(), dto.getPhone(), dto.getEmail());
        return Result.success();
    }

    @Operation(summary = "用户登录")
    @PostMapping("/auth/login")
    public Result<LoginVO> login(@Validated @RequestBody LoginDTO dto) {
        String token = userService.login(dto.getUsername(), dto.getPassword());
        User user = userService.getById(LoginUserContext.getCurrentUserId());
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setRole(user.getRole());
        vo.setNickname(user.getNickname());
        return Result.success(vo);
    }

    @Operation(summary = "获取当前用户信息")
    @RequireRole(1)
    @GetMapping("/user/profile")
    public Result<UserProfileVO> profile() {
        Long userId = LoginUserContext.getCurrentUserId();
        User user = userService.getById(userId);
        UserProfileVO vo = new UserProfileVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        return Result.success(vo);
    }

    @Operation(summary = "更新个人信息")
    @RequireRole(1)
    @PutMapping("/user/profile")
    public Result<Void> updateProfile(@Validated @RequestBody UpdateProfileDTO dto) {
        Long userId = LoginUserContext.getCurrentUserId();
        userService.updateProfile(userId, dto.getNickname(), dto.getAvatar(), dto.getPhone(), dto.getEmail());
        return Result.success();
    }

    @Operation(summary = "收货地址列表")
    @RequireRole(1)
    @GetMapping("/user/addresses")
    public Result<List<Address>> addresses() {
        Long userId = LoginUserContext.getCurrentUserId();
        return Result.success(addressService.listByUser(userId));
    }

    @Operation(summary = "新增收货地址")
    @RequireRole(1)
    @PostMapping("/user/addresses")
    public Result<Long> addAddress(@Validated @RequestBody Address address) {
        Long userId = LoginUserContext.getCurrentUserId();
        Long id = addressService.add(userId, address);
        return Result.success(id);
    }

    @Operation(summary = "更新收货地址")
    @RequireRole(1)
    @PutMapping("/user/addresses/{id}")
    public Result<Void> updateAddress(@PathVariable Long id, @Validated @RequestBody Address address) {
        Long userId = LoginUserContext.getCurrentUserId();
        addressService.update(userId, id, address);
        return Result.success();
    }

    @Operation(summary = "删除收货地址")
    @RequireRole(1)
    @DeleteMapping("/user/addresses/{id}")
    public Result<Void> deleteAddress(@PathVariable Long id) {
        Long userId = LoginUserContext.getCurrentUserId();
        addressService.delete(userId, id);
        return Result.success();
    }
}
