package com.pzhu.mall.modules.admin.controller;

import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.common.result.PageResult;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.modules.user.entity.User;
import com.pzhu.mall.modules.user.mapper.UserMapper;
import com.pzhu.mall.modules.admin.service.OperationLogService;
import com.pzhu.mall.modules.admin.vo.AdminUserVO;
import com.pzhu.mall.security.LoginUserContext;
import com.pzhu.mall.security.RequireRole;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

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

    @Resource
    private OperationLogService operationLogService;

    @Resource
    private com.pzhu.mall.security.AccountStatusService accountStatusService;

    @Operation(summary = "用户列表")
    @GetMapping
    public Result<PageResult<AdminUserVO>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                                @RequestParam(defaultValue = "10") Integer pageSize,
                                                @RequestParam(required = false) Integer role,
                                                @RequestParam(required = false) Integer status,
                                                @RequestParam(required = false) String keyword) {
        var qw = new LambdaQueryWrapper<User>()
                .eq(User::getIsDeleted, 0);
        if (role != null) {
            qw.eq(User::getRole, role);
        }
        if (status != null) {
            qw.eq(User::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(User::getUsername, keyword).or().like(User::getNickname, keyword));
        }
        qw.orderByDesc(User::getCreateTime);

        Page<User> page = new Page<>(pageNum, pageSize);
        var result = userMapper.selectPage(page, qw);
        List<AdminUserVO> voList = result.getRecords().stream()
                .map(AdminUserVO::from)
                .collect(Collectors.toList());
        long pages = (long) Math.ceil((double) result.getTotal() / pageSize);
        var pageResult = new PageResult<AdminUserVO>(result.getTotal(), pageNum, pageSize, pages, voList);
        return Result.success(pageResult);
    }

    @Operation(summary = "禁用/启用用户")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusDTO dto) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        Long operatorId = LoginUserContext.getCurrentUserId();
        boolean disabling = !Integer.valueOf(1).equals(dto.getStatus());
        if (disabling) {
            // H-03 修复：禁止管理员禁用自己，也禁止禁用其他管理员账号
            if (id.equals(operatorId)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "不能禁用自己的账号");
            }
            if (user.getRole() != null && user.getRole() == 3) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "不能禁用其他管理员账号");
            }
        }
        user.setStatus(dto.getStatus());
        userMapper.updateById(user);
        // H-2 修复：清除账号状态缓存，使禁用/启用立即对该用户已签发的 JWT 生效
        accountStatusService.evict(id);

        operationLogService.record(operatorId,
                Integer.valueOf(1).equals(dto.getStatus()) ? "启用用户" : "禁用用户",
                "用户#" + id);
        return Result.success();
    }

    @Operation(summary = "用户角色分配（FR-A-01）")
    @PutMapping("/{id}/role")
    public Result<Void> updateRole(@PathVariable Long id, @Valid @RequestBody RoleDTO dto) {
        User user = userMapper.selectById(id);
        if (user == null || Integer.valueOf(1).equals(user.getIsDeleted())) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        Integer role = dto.getRole();
        if (role == null || (role != 1 && role != 2 && role != 3)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "角色仅支持 1=消费者/2=商家/3=管理员");
        }
        Long operatorId = LoginUserContext.getCurrentUserId();
        // 防自降级：管理员不能把自己的角色从 3 改走（保持管理员账户稳定性）
        if (id.equals(operatorId) && role != 3) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能修改自己的管理员角色");
        }
        user.setRole(role);
        userMapper.updateById(user);
        // 角色变更后旧 JWT 中的角色声明失效：清除账号状态缓存，强制重新登录
        accountStatusService.evict(id);
        operationLogService.record(operatorId, "分配用户角色", "用户#" + id + " → 角色" + role);
        return Result.success();
    }

    public static class RoleDTO {
        @javax.validation.constraints.NotNull(message = "role 不能为空")
        private Integer role;
        public Integer getRole() { return role; }
        public void setRole(Integer role) { this.role = role; }
    }

    public static class StatusDTO {
        @javax.validation.constraints.NotNull(message = "status 不能为空")
        @javax.validation.constraints.Min(value = 0, message = "status 仅支持 0/1")
        @javax.validation.constraints.Max(value = 1, message = "status 仅支持 0/1")
        private Integer status;
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
    }
}
