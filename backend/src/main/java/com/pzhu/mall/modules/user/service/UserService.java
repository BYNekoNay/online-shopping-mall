package com.pzhu.mall.modules.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.modules.user.entity.Address;
import com.pzhu.mall.modules.user.entity.User;
import com.pzhu.mall.modules.user.mapper.UserMapper;
import com.pzhu.mall.security.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private final UserMapper userMapper;
    private final AddressService addressService;
    private final JwtUtil jwtUtil;

    public UserService(UserMapper userMapper, AddressService addressService, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.addressService = addressService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 用户注册。
     */
    @Transactional
    public Long register(String username, String rawPassword, String nickname) {
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        qw.eq(User::getUsername, username).eq(User::getIsDeleted, 0);
        if (userMapper.selectCount(qw) > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(ENCODER.encode(rawPassword));
        user.setNickname(nickname != null ? nickname : username);
        user.setRole(1); // 消费者
        user.setStatus(1);
        user.setPoints(0);
        user.setIsDeleted(0);
        userMapper.insert(user);
        return user.getId();
    }

    /**
     * 用户登录，返回 JWT Token。
     */
    public String login(String username, String rawPassword) {
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        qw.eq(User::getUsername, username).eq(User::getIsDeleted, 0);
        User user = userMapper.selectOne(qw);

        if (user == null || !ENCODER.matches(rawPassword, user.getPassword())) {
            throw new BusinessException(ErrorCode.USERNAME_OR_PASSWORD_ERROR);
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
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
     * 更新用户个人信息。
     */
    @Transactional
    public void updateProfile(Long userId, String nickname, String avatar, String phone, String email) {
        LambdaUpdateWrapper<User> uw = new LambdaUpdateWrapper<>();
        uw.set(User::getNickname, nickname)
          .set(User::getAvatar, avatar)
          .set(User::getPhone, phone)
          .set(User::getEmail, email)
          .eq(User::getId, userId)
          .eq(User::getIsDeleted, 0);
        userMapper.update(null, uw);
    }
}
