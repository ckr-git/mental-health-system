package com.mental.health.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mental.health.entity.User;
import com.mental.health.mapper.UserMapper;
import com.mental.health.mapper.MoodDiaryMapper;
import com.mental.health.mapper.AssessmentReportMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 用户服务
 */
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired(required = false)
    private MoodDiaryMapper moodDiaryMapper;

    @Autowired(required = false)
    private AssessmentReportMapper assessmentReportMapper;

    @Value("${file.upload-path:./uploads/}")
    private String uploadPath;

    /**
     * 获取用户信息
     */
    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }

    /**
     * 更新用户信息
     */
    public boolean updateUser(User user) {
        // 不允许通过此方法修改密码
        user.setPassword(null);
        return userMapper.updateById(user) > 0;
    }

    /**
     * 修改密码
     */
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return false;
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return false;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        return userMapper.updateById(user) > 0;
    }

    /**
     * 获取用户列表（管理员）
     */
    public IPage<User> getUserList(int pageNum, int pageSize, String role, String keyword) {
        Page<User> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(role)) {
            queryWrapper.eq(User::getRole, role);
        }
        
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                    .like(User::getUsername, keyword)
                    .or()
                    .like(User::getNickname, keyword)
                    .or()
                    .like(User::getPhone, keyword)
                    .or()
                    .like(User::getEmail, keyword));
        }
        
        queryWrapper.orderByDesc(User::getCreateTime);
        
        return userMapper.selectPage(page, queryWrapper);
    }

    /**
     * 禁用/启用用户
     * status: 0=待审核, 1=正常, 2=禁用
     */
    public boolean toggleUserStatus(Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            // 如果当前是正常状态(1)，则禁用(2)；否则启用(1)
            user.setStatus(user.getStatus() == 1 ? 2 : 1);
            return userMapper.updateById(user) > 0;
        }
        return false;
    }

    /**
     * 删除用户（逻辑删除）
     */
    public boolean deleteUser(Long userId) {
        return userMapper.deleteById(userId) > 0;
    }

    /**
     * 获取统计数据
     */
    public Map<String, Long> getUserStatistics() {
        Map<String, Long> statistics = new HashMap<>();
        
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        statistics.put("totalUsers", userMapper.selectCount(queryWrapper));
        
        queryWrapper.clear();
        queryWrapper.eq(User::getRole, "PATIENT");
        statistics.put("patientCount", userMapper.selectCount(queryWrapper));
        
        queryWrapper.clear();
        queryWrapper.eq(User::getRole, "DOCTOR");
        statistics.put("doctorCount", userMapper.selectCount(queryWrapper));
        
        queryWrapper.clear();
        queryWrapper.eq(User::getStatus, 1);
        statistics.put("activeUsers", userMapper.selectCount(queryWrapper));
        
        return statistics;
    }

    /**
     * 获取医生列表
     */
    public IPage<User> getDoctorList(int pageNum, int pageSize) {
        Page<User> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getRole, "DOCTOR")
                .eq(User::getStatus, 1)
                .orderByDesc(User::getCreateTime);
        return userMapper.selectPage(page, queryWrapper);
    }

    /**
     * 审核医生（批准或拒绝）
     */
    public boolean approveDoctor(Long doctorId, boolean approve) {
        User user = userMapper.selectById(doctorId);
        if (user == null || !"DOCTOR".equals(user.getRole())) {
            return false;
        }

        if (approve) {
            // 批准：设置status=1
            user.setStatus(1);
        } else {
            // 拒绝：设置status=2（禁用）
            user.setStatus(2);
        }

        return userMapper.updateById(user) > 0;
    }

    /**
     * 获取医生工作统计
     */
    public Map<String, Object> getDoctorStatistics(Long doctorId) {
        Map<String, Object> statistics = new HashMap<>();

        User doctor = userMapper.selectById(doctorId);
        if (doctor == null || !"DOCTOR".equals(doctor.getRole())) {
            return statistics;
        }

        // 基本信息
        statistics.put("doctorId", doctorId);
        statistics.put("doctorName", doctor.getNickname());
        statistics.put("specialization", doctor.getSpecialization());
        statistics.put("status", doctor.getStatus());

        // TODO: 这些统计数据需要从其他表查询
        // 患者数、咨询次数、评估报告数等
        statistics.put("patientCount", 0);
        statistics.put("consultationCount", 0);
        statistics.put("reportCount", 0);
        statistics.put("averageRating", 0.0);

        return statistics;
    }

    /**
     * 获取用户统计信息
     */
    public Map<String, Object> getUserStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();

        User user = userMapper.selectById(userId);
        if (user == null) {
            return stats;
        }

        // 计算使用天数
        if (user.getCreateTime() != null) {
            long days = ChronoUnit.DAYS.between(user.getCreateTime(), LocalDateTime.now());
            stats.put("daysCount", days);
        } else {
            stats.put("daysCount", 0);
        }

        // 情绪日记数量
        if (moodDiaryMapper != null) {
            QueryWrapper wrapper = new QueryWrapper();
            wrapper.eq("user_id", userId);
            stats.put("symptomsCount", moodDiaryMapper.selectCount(wrapper));
        } else {
            stats.put("symptomsCount", 0);
        }

        // 评估报告数量
        if (assessmentReportMapper != null) {
            QueryWrapper wrapper = new QueryWrapper();
            wrapper.eq("user_id", userId);
            stats.put("reportsCount", assessmentReportMapper.selectCount(wrapper));
        } else {
            stats.put("reportsCount", 0);
        }

        return stats;
    }

    /**
     * 上传头像
     */
    public String uploadAvatar(Long userId, MultipartFile file) throws IOException {
        // 创建上传目录
        File uploadDir = new File(uploadPath + "/avatars");
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
        String filename = userId + "_" + UUID.randomUUID().toString() + extension;

        // 保存文件
        Path filePath = Paths.get(uploadDir.getAbsolutePath(), filename);
        Files.write(filePath, file.getBytes());

        // 更新用户头像URL
        String avatarUrl = "/uploads/avatars/" + filename;
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setAvatar(avatarUrl);
            userMapper.updateById(user);
        }

        return avatarUrl;
    }

    /**
     * 修改手机号
     */
    public boolean changePhone(Long userId, String newPhone) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return false;
        }

        user.setPhone(newPhone);
        return userMapper.updateById(user) > 0;
    }
}
