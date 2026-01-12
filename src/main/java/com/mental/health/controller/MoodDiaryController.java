package com.mental.health.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mental.health.common.JwtUtil;
import com.mental.health.common.Result;
import com.mental.health.entity.MoodDiary;
import com.mental.health.service.MoodDiaryService;
import com.mental.health.service.UserThemeConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 情绪日记控制器
 */
@RestController
@RequestMapping("/api/patient/mood-diary")
@PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
public class MoodDiaryController {

    @Autowired
    private MoodDiaryService diaryService;

    @Autowired
    private UserThemeConfigService themeConfigService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 添加日记
     */
    @PostMapping("/add")
    public Result<MoodDiary> addDiary(@RequestBody MoodDiary diary, @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        diary.setUserId(userId);
        
        MoodDiary result = diaryService.addDiary(diary);
        
        // 增加统计计数
        themeConfigService.incrementDiaryCount(userId);
        
        return Result.success("日记添加成功", result);
    }

    /**
     * 获取日记列表（分页）
     */
    @GetMapping("/list")
    public Result<Page<MoodDiary>> getDiaryList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        Page<MoodDiary> page = diaryService.getDiaryList(userId, pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 获取日记详情
     */
    @GetMapping("/detail/{id}")
    public Result<MoodDiary> getDiaryDetail(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        MoodDiary diary = diaryService.getDiaryDetail(id, userId);
        if (diary != null) {
            return Result.success(diary);
        }
        return Result.error("日记不存在");
    }

    /**
     * 更新状态
     */
    @PutMapping("/status/{id}")
    public Result<String> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> params,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        String status = params.get("status");
        
        if (status == null || !isValidStatus(status)) {
            return Result.error("无效的状态值");
        }
        
        boolean success = diaryService.updateStatus(id, userId, status);
        if (success) {
            return Result.success("状态更新成功");
        }
        return Result.error("状态更新失败");
    }

    /**
     * 获取用户统计数据
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getUserStats(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        Map<String, Object> stats = diaryService.getUserStats(userId);
        return Result.success(stats);
    }

    /**
     * 获取最近的日记
     */
    @GetMapping("/recent")
    public Result<List<MoodDiary>> getRecentDiaries(
            @RequestParam(defaultValue = "10") int limit,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        List<MoodDiary> diaries = diaryService.getRecentDiaries(userId, limit);
        return Result.success(diaries);
    }

    /**
     * 验证状态值是否有效
     */
    private boolean isValidStatus(String status) {
        return "ongoing".equals(status) || "better".equals(status) 
                || "overcome".equals(status) || "proud".equals(status);
    }
}
