package com.mental.health.controller;

import com.mental.health.common.JwtUtil;
import com.mental.health.common.Result;
import com.mental.health.entity.MoodComment;
import com.mental.health.service.MoodCommentService;
import com.mental.health.service.UserThemeConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 心情留言控制器
 */
@RestController
@RequestMapping("/api/patient/mood-comment")
@PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
public class MoodCommentController {

    @Autowired
    private MoodCommentService commentService;

    @Autowired
    private UserThemeConfigService themeConfigService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 添加留言
     */
    @PostMapping("/add")
    public Result<MoodComment> addComment(@RequestBody MoodComment comment, @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        comment.setUserId(userId);
        
        MoodComment result = commentService.addComment(comment);
        
        // 增加统计计数
        themeConfigService.incrementCommentCount(userId);
        
        return Result.success("留言添加成功", result);
    }

    /**
     * 获取日记的留言列表
     */
    @GetMapping("/list/{diaryId}")
    public Result<List<MoodComment>> getCommentList(@PathVariable Long diaryId) {
        List<MoodComment> comments = commentService.getCommentsByDiaryId(diaryId);
        return Result.success(comments);
    }

    /**
     * 更新互动标记
     */
    @PutMapping("/interaction/{commentId}")
    public Result<String> updateInteraction(
            @PathVariable Long commentId,
            @RequestBody Map<String, Object> params,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        
        @SuppressWarnings("unchecked")
        List<String> interactions = (List<String>) params.get("interactions");
        
        boolean success = commentService.updateInteractions(commentId, userId, interactions);
        if (success) {
            return Result.success("互动更新成功");
        }
        return Result.error("互动更新失败");
    }

    /**
     * 删除留言
     */
    @DeleteMapping("/{commentId}")
    public Result<String> deleteComment(@PathVariable Long commentId, @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        boolean success = commentService.deleteComment(commentId, userId);
        if (success) {
            return Result.success("留言删除成功");
        }
        return Result.error("留言删除失败");
    }
}
