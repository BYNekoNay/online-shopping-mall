package com.pzhu.mall.modules.admin.controller;

import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.common.result.PageResult;
import com.pzhu.mall.modules.user.entity.User;
import com.pzhu.mall.modules.user.mapper.UserMapper;
import com.pzhu.mall.security.RequireRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 管理员端用户管理控制器。
 */
@Tag(name = "管理员-用户管理")
@RestController
@RequestMapping("/api/admin/users")
@RequireRole(3)
public class AdminUserController {

    @Resource
    private UserMapper userMapper;

    @Operation(summary = "用户列表")
    @GetMapping
    public Result<PageResult<User>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                         @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<User> page = new Page<>(pageNum, pageSize);
        return Result.success(PageResult.of(userMapper.selectPage(page, null)));
    }

    @Operation(summary = "禁用/启用用户")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody StatusDTO dto) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.error(404, "User not found");
        }
        user.setStatus(dto.getStatus());
        userMapper.updateById(user);
        return Result.success();
    }

    public static class StatusDTO {
        private Integer status;
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
    }
}
