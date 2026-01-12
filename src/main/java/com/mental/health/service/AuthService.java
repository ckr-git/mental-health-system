package com.mental.health.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mental.health.common.JwtUtil;
import com.mental.health.entity.User;
import com.mental.health.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证服务
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private LoginAttemptService loginAttemptService;

    /**
     * 用户注册
     */
    public boolean register(User user) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, user.getUsername());
        Long count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            return false;
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 特殊处理：admin用户名自动设置为管理员
        if ("admin".equals(user.getUsername())) {
            user.setRole("ADMIN");
        }

        // 设置角色，默认为患者
        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("PATIENT");
        }

        // 根据角色设置状态
        // 医生注册需要审核（status=0），患者和管理员注册直接激活（status=1）
        if ("DOCTOR".equals(user.getRole())) {
            user.setStatus(0);  // 待审核
        } else {
            user.setStatus(1);  // 患者和管理员直接激活
        }

        return userMapper.insert(user) > 0;
    }

    /**
     * 用户登录
     */
    public Map<String, Object> login(String username, String password) {
        // 检查账户是否被锁定
        if (loginAttemptService.isLocked(username)) {
            int remaining = loginAttemptService.getRemainingLockTime(username);
            logger.warn("账户 {} 已被锁定，剩余 {} 秒", username, remaining);
            Map<String, Object> lockedResult = new HashMap<>();
            lockedResult.put("locked", true);
            lockedResult.put("lockTime", remaining);
            return lockedResult;
        }

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            loginAttemptService.loginFailed(username);
            logger.warn("登录失败：用户 {} 不存在", username);
            return null;
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            loginAttemptService.loginFailed(username);
            int remaining = loginAttemptService.getRemainingAttempts(username);
            logger.warn("登录失败：用户 {} 密码错误，剩余尝试次数 {}", username, remaining);
            return null;
        }

        // 检查账户状态
        if (user.getStatus() == 0) {
            Map<String, Object> pendingResult = new HashMap<>();
            pendingResult.put("pending", true);
            return pendingResult;
        }

        if (user.getStatus() == 2) {
            logger.warn("登录失败：用户 {} 账户已被禁用", username);
            return null;
        }

        // 登录成功，清除失败记录
        loginAttemptService.loginSucceeded(username);
        logger.info("用户 {} 登录成功", username);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userInfo", getUserInfo(user));

        return result;
    }

    private Map<String, Object> getUserInfo(User user) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("phone", user.getPhone());
        userInfo.put("email", user.getEmail());
        userInfo.put("role", user.getRole());
        userInfo.put("gender", user.getGender());
        userInfo.put("age", user.getAge());
        return userInfo;
    }
}
