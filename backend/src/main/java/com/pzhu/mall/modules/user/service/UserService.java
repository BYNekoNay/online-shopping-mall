package com.pzhu.mall.modules.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.modules.user.entity.User;
import com.pzhu.mall.modules.user.mapper.UserMapper;
import com.pzhu.mall.security.JwtUtil;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final LoginAttemptService loginAttemptService;

    public UserService(UserMapper userMapper, JwtUtil jwtUtil, LoginAttemptService loginAttemptService) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
        this.loginAttemptService = loginAttemptService;
    }

    /**
     * 用户注册。
     */
    @Transactional
    public Long register(String username, String rawPassword, String nickname, String phone, String email) {
        // M12 修复：密码强度校验（至少 8 位）
        if (rawPassword == null || rawPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "密码长度至少 8 位");
        }
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        qw.eq(User::getUsername, username).eq(User::getIsDeleted, 0);
        if (userMapper.selectCount(qw) > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
        if (phone != null && !phone.isBlank()) {
            LambdaQueryWrapper<User> pw = new LambdaQueryWrapper<>();
            pw.eq(User::getPhone, phone).eq(User::getIsDeleted, 0);
            if (userMapper.selectCount(pw) > 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "该手机号已被注册");
            }
        }
        if (email != null && !email.isBlank()) {
            LambdaQueryWrapper<User> ew = new LambdaQueryWrapper<>();
            ew.eq(User::getEmail, email).eq(User::getIsDeleted, 0);
            if (userMapper.selectCount(ew) > 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "该邮箱已被注册");
            }
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(ENCODER.encode(rawPassword));
        user.setNickname(nickname != null ? nickname : username);
        user.setPhone(phone);
        user.setEmail(email);
        user.setRole(1); // 消费者
        user.setStatus(1);
        user.setPoints(0);
        user.setIsDeleted(0);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("username") || msg.contains("uk_username")) {
                throw new BusinessException(ErrorCode.USERNAME_EXISTS);
            } else if (msg.contains("phone") || msg.contains("uk_phone")) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "该手机号已被注册");
            } else if (msg.contains("email") || msg.contains("uk_email")) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "该邮箱已被注册");
            }
            throw new BusinessException(ErrorCode.PARAM_ERROR, "注册信息冲突，请检查输入");
        }
        return user.getId();
    }

    /**
     * 用户登录，返回 JWT Token。
     */
    public String login(String username, String rawPassword) {
        // 检查是否被锁定
        loginAttemptService.checkAllowed(username);

        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        qw.eq(User::getUsername, username).eq(User::getIsDeleted, 0);
        User user = userMapper.selectOne(qw);

        if (user == null || !ENCODER.matches(rawPassword, user.getPassword())) {
            loginAttemptService.recordFailure(username);
            throw new BusinessException(ErrorCode.USERNAME_OR_PASSWORD_ERROR);
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
        // 登录成功，清除失败计数
        loginAttemptService.clear(username);
        return jwtUtil.generateToken(user.getId(), user.getRole());
    }

    /**
     * 获取用户信息。
     */
    public User getById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return user;
    }

    /**
     * 更新用户个人信息（仅更新非 null 字段，避免覆盖已有数据）。
     * <p>U-01 修复：更新手机号/邮箱前校验唯一性（排除自身），避免 DuplicateKeyException
     * 落入全局 500；同时保留 DB 唯一索引兜底。</p>
     */
    @Transactional
    public void updateProfile(Long userId, String nickname, String avatar, String phone, String email) {
        if (phone != null && !phone.isBlank()) {
            LambdaQueryWrapper<User> pw = new LambdaQueryWrapper<>();
            pw.eq(User::getPhone, phone).eq(User::getIsDeleted, 0).ne(User::getId, userId);
            if (userMapper.selectCount(pw) > 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "该手机号已被他人使用");
            }
        }
        if (email != null && !email.isBlank()) {
            LambdaQueryWrapper<User> ew = new LambdaQueryWrapper<>();
            ew.eq(User::getEmail, email).eq(User::getIsDeleted, 0).ne(User::getId, userId);
            if (userMapper.selectCount(ew) > 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "该邮箱已被他人使用");
            }
        }
        LambdaUpdateWrapper<User> uw = new LambdaUpdateWrapper<>();
        if (nickname != null) uw.set(User::getNickname, nickname);
        if (avatar != null) uw.set(User::getAvatar, avatar);
        if (phone != null) uw.set(User::getPhone, phone);
        if (email != null) uw.set(User::getEmail, email);
        uw.eq(User::getId, userId).eq(User::getIsDeleted, 0);
        userMapper.update(null, uw);
    }
}
