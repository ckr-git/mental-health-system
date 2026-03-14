package com.mental.health.controller;

import com.mental.health.common.JwtUtil;
import com.mental.health.common.Result;
import com.mental.health.entity.TimeCapsule;
import com.mental.health.service.TimeCapsuleService;
import com.mental.health.service.UserThemeConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 时光信箱控制器
 */
@RestController
@RequestMapping("/api/patient/time-capsule")
@PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
public class TimeCapsuleController {

    @Autowired
    private TimeCapsuleService capsuleService;

    @Autowired
    private UserThemeConfigService themeConfigService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 写信 (兼容/write和/create两个路径)
     */
    @PostMapping({"/write", "/create"})
    public Result<TimeCapsule> writeLetter(@RequestBody TimeCapsule capsule, @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));

        if (capsule.getUnlockDate() == null) {
            return Result.error("请设置解锁日期");
        }

        TimeCapsule result = capsuleService.writeLetter(capsule, userId);

        // 增加统计计数
        themeConfigService.incrementLetterCount(userId);

        return Result.success("信件已寄出", result);
    }

    /**
     * 获取信箱列表
     */
    @GetMapping("/list")
    public Result<List<TimeCapsule>> getLetterList(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        // 先检查并解锁满足条件的信件
        capsuleService.checkAndUnlockLetters(userId);
        // 再获取列表
        List<TimeCapsule> letters = capsuleService.getUserLetters(userId);
        return Result.success(letters);
    }

    /**
     * 获取可解锁的信件列表
     */
    @GetMapping("/unlockable")
    public Result<List<TimeCapsule>> getUnlockableLetters(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        List<TimeCapsule> letters = capsuleService.checkAndUnlockLetters(userId);
        return Result.success(letters);
    }

    /**
     * 检查可解锁的信件
     */
    @GetMapping("/check-unlock")
    public Result<List<TimeCapsule>> checkUnlockLetters(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        List<TimeCapsule> letters = capsuleService.checkAndUnlockLetters(userId);
        if (!letters.isEmpty()) {
            return Result.success("有 " + letters.size() + " 封信件可以打开了", letters);
        }
        return Result.success("暂无可解锁的信件", letters);
    }

    /**
     * 解锁信件
     */
    @GetMapping("/unlock/{id}")
    public Result<TimeCapsule> unlockLetter(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        TimeCapsule letter = capsuleService.unlockLetter(id, userId);
        if (letter != null) {
            return Result.success("信件已解锁", letter);
        }
        return Result.error("解锁失败");
    }

    /**
     * 阅读信件
     */
    @PostMapping("/read/{id}")
    public Result<TimeCapsule> readLetter(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        TimeCapsule letter = capsuleService.readLetter(id, userId);
        if (letter != null) {
            return Result.success(letter);
        }
        return Result.error("阅读失败");
    }

    /**
     * 回复信件
     */
    @PostMapping("/reply/{id}")
    public Result<String> replyLetter(
            @PathVariable Long id,
            @RequestBody Map<String, String> params,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        String replyContent = params.get("replyContent");
        
        boolean success = capsuleService.replyLetter(id, userId, replyContent);
        if (success) {
            return Result.success("回复成功");
        }
        return Result.error("回复失败");
    }

    /**
     * 获取信件详情
     */
    @GetMapping("/detail/{id}")
    public Result<TimeCapsule> getLetterDetail(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        TimeCapsule letter = capsuleService.getLetterDetail(id, userId);
        if (letter != null) {
            return Result.success(letter);
        }
        return Result.error("信件不存在");
    }

    /**
     * 获取智能推荐
     */
    @GetMapping("/recommend")
    public Result<com.mental.health.entity.LetterRecommendation> getRecommendation(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        com.mental.health.entity.LetterRecommendation recommendation = capsuleService.analyzeAndRecommend(userId);
        return Result.success(recommendation);
    }
}
