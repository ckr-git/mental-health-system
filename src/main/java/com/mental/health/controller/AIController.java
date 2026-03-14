package com.mental.health.controller;

import com.mental.health.common.JwtUtil;
import com.mental.health.common.Result;
import com.mental.health.entity.AIConversation;
import com.mental.health.service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI智能助手控制器
 */
@RestController
@RequestMapping("/api/patient/ai")
@PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
public class AIController {

    @Autowired
    private AIService aiService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * AI问答
     */
    @PostMapping("/ask")
    public Result<String> askQuestion(@RequestBody Map<String, String> request, @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        String question = request.get("question");
        
        if (question == null || question.trim().isEmpty()) {
            return Result.error("问题不能为空");
        }

        String answer = aiService.askQuestion(userId, question);
        return Result.success(answer);
    }

    /**
     * 获取对话历史
     */
    @GetMapping("/conversations")
    public Result<List<AIConversation>> getConversations(
            @RequestParam(defaultValue = "20") int limit,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        List<AIConversation> conversations = aiService.getUserConversations(userId, limit);
        return Result.success(conversations);
    }

    /**
     * 提交反馈
     */
    @PostMapping("/feedback")
    public Result<String> submitFeedback(@RequestBody Map<String, Object> request) {
        Long conversationId = Long.valueOf(request.get("conversationId").toString());
        Integer feedback = Integer.valueOf(request.get("feedback").toString());
        
        boolean success = aiService.submitFeedback(conversationId, feedback);
        if (success) {
            return Result.success("反馈提交成功");
        } else {
            return Result.error("反馈提交失败");
        }
    }

    /**
     * 生成AI评估报告
     */
    @PostMapping("/generate-report")
    public Result<String> generateReport(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        String report = aiService.generateAssessmentReport(userId);
        return Result.success(report);
    }
}
