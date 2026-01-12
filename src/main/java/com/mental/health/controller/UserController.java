package com.mental.health.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mental.health.common.JwtUtil;
import com.mental.health.common.Result;
import com.mental.health.entity.User;
import com.mental.health.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 获取当前用户信息
     */
    @GetMapping("/user/profile")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    public Result<User> getProfile(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        User user = userService.getUserById(userId);
        
        if (user != null) {
            user.setPassword(null); // 不返回密码
            return Result.success(user);
        } else {
            return Result.error("用户不存在");
        }
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/user/profile")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    public Result<String> updateProfile(@RequestBody User user, @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        user.setId(userId);
        
        boolean success = userService.updateUser(user);
        if (success) {
            return Result.success("更新成功");
        } else {
            return Result.error("更新失败");
        }
    }

    /**
     * 修改密码
     */
    @PostMapping("/user/change-password")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    public Result<String> changePassword(@RequestBody Map<String, String> request, @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        String oldPassword = request.get("oldPassword");
        String newPassword = request.get("newPassword");

        boolean success = userService.changePassword(userId, oldPassword, newPassword);
        if (success) {
            return Result.success("密码修改成功");
        } else {
            return Result.error("原密码错误");
        }
    }

    /**
     * 获取用户统计信息
     */
    @GetMapping("/user/stats")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    public Result<Map<String, Object>> getUserStats(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        Map<String, Object> stats = userService.getUserStats(userId);
        return Result.success(stats);
    }

    /**
     * 上传头像
     */
    @PostMapping("/user/avatar")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    public Result<Map<String, String>> uploadAvatar(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));

        try {
            String avatarUrl = userService.uploadAvatar(userId, file);
            Map<String, String> result = new java.util.HashMap<>();
            result.put("url", avatarUrl);
            return Result.success("上传成功", result);
        } catch (Exception e) {
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    /**
     * 修改手机号
     */
    @PostMapping("/user/change-phone")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    public Result<String> changePhone(@RequestBody Map<String, String> request, @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        String newPhone = request.get("newPhone");
        String code = request.get("code");

        // TODO: 实际项目中需要验证验证码
        boolean success = userService.changePhone(userId, newPhone);
        if (success) {
            return Result.success("手机号修改成功");
        } else {
            return Result.error("手机号修改失败");
        }
    }

    /**
     * 获取用户列表（管理员）
     */
    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<IPage<User>> getUserList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String keyword) {
        IPage<User> users = userService.getUserList(pageNum, pageSize, role, keyword);
        return Result.success(users);
    }

    /**
     * 切换用户状态（管理员）
     */
    @PostMapping("/admin/users/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> toggleUserStatus(@PathVariable Long id) {
        boolean success = userService.toggleUserStatus(id);
        if (success) {
            return Result.success("操作成功");
        } else {
            return Result.error("操作失败");
        }
    }

    /**
     * 删除用户（管理员）
     */
    @DeleteMapping("/admin/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> deleteUser(@PathVariable Long id) {
        boolean success = userService.deleteUser(id);
        if (success) {
            return Result.success("删除成功");
        } else {
            return Result.error("删除失败");
        }
    }

    /**
     * 获取用户统计（管理员）
     */
    @GetMapping("/admin/users/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Long>> getUserStatistics() {
        Map<String, Long> statistics = userService.getUserStatistics();
        return Result.success(statistics);
    }

    /**
     * 获取医生列表
     */
    @GetMapping("/patient/doctors")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    public Result<IPage<User>> getDoctorList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        IPage<User> doctors = userService.getDoctorList(pageNum, pageSize);
        return Result.success(doctors);
    }

    /**
     * 审核医生（管理员）
     */
    @PostMapping("/admin/doctors/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> approveDoctor(
            @PathVariable Long id,
            @RequestParam boolean approve) {
        boolean success = userService.approveDoctor(id, approve);
        if (success) {
            return Result.success(approve ? "医生审核通过" : "医生审核拒绝");
        } else {
            return Result.error("操作失败");
        }
    }

    /**
     * 更新医生信息（管理员）
     */
    @PutMapping("/admin/doctors/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> updateDoctor(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        boolean success = userService.updateUser(user);
        if (success) {
            return Result.success("更新成功");
        } else {
            return Result.error("更新失败");
        }
    }

    /**
     * 获取医生工作统计（管理员）
     */
    @GetMapping("/admin/doctors/{id}/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getDoctorStatistics(@PathVariable Long id) {
        Map<String, Object> statistics = userService.getDoctorStatistics(id);
        return Result.success(statistics);
    }
}
