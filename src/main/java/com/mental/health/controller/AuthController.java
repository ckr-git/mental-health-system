package com.mental.health.controller;

import com.mental.health.common.Result;
import com.mental.health.entity.User;
import com.mental.health.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<String> register(@RequestBody User user) {
        boolean success = authService.register(user);
        if (success) {
            return Result.success("注册成功");
        } else {
            return Result.error("用户名已存在");
        }
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> loginData) {
        String username = loginData.get("username");
        String password = loginData.get("password");

        Map<String, Object> result = authService.login(username, password);
        if (result != null) {
            // 检查是否被锁定
            if (result.containsKey("locked")) {
                int lockTime = (int) result.get("lockTime");
                return Result.error("账户已被锁定，请 " + lockTime + " 秒后重试");
            }
            // 检查是否是待审核状态
            if (result.containsKey("pending")) {
                return Result.error("您的医生账号正在审核中，请耐心等待管理员审核（预计1-2天）");
            }
            return Result.success("登录成功", result);
        } else {
            return Result.error("用户名或密码错误");
        }
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public Result<String> logout() {
        return Result.success("登出成功");
    }

    /**
     * 发送验证码
     */
    @PostMapping("/send-code")
    public Result<String> sendCode(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");
        // TODO: 实际项目中需要调用短信服务发送验证码
        // 目前仅返回成功，验证码为固定值 "123456"
        return Result.success("验证码已发送");
    }
}
