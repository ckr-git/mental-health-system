package com.mental.health.controller;

import com.mental.health.common.Result;
import com.mental.health.entity.ConsultationSession;
import com.mental.health.entity.User;
import com.mental.health.mapper.UserMapper;
import com.mental.health.service.ChatMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 聊天REST API控制器
 */
@RestController
@RequestMapping("/api")
public class ChatRestController {

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private UserMapper userMapper;

    /**
     * 获取当前用户信息
     */
    private User getCurrentUser(Authentication auth) {
        String username = auth.getName();
        return userMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
    }

    /**
     * 患者端 - 获取聊天列表
     */
    @GetMapping("/patient/chat/list")
    public Result<List<Map<String, Object>>> getPatientChatList(Authentication auth) {
        User user = getCurrentUser(auth);
        List<Map<String, Object>> list = chatMessageService.getChatList(user.getId(), "PATIENT");
        return Result.success(list);
    }

    /**
     * 患者端 - 创建聊天会话
     */
    @PostMapping("/patient/chat/create")
    public Result<Map<String, Object>> createChat(
            @RequestBody Map<String, Object> params,
            Authentication auth) {
        User patient = getCurrentUser(auth);
        Long doctorId = Long.valueOf(params.get("doctorId").toString());

        ConsultationSession session = chatMessageService.getOrCreateSession(
            patient.getId(), doctorId);

        // 获取医生信息
        User doctor = userMapper.selectById(doctorId);

        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", session.getId());
        result.put("userId", doctor.getId());
        result.put("name", doctor.getNickname() != null ?
            doctor.getNickname() : doctor.getUsername());
        result.put("avatar", doctor.getAvatar());
        result.put("unreadCount", 0);

        return Result.success(result);
    }

    /**
     * 患者端 - 获取消息历史
     */
    @GetMapping("/patient/chat/messages/{sessionId}")
    public Result<Map<String, Object>> getPatientMessages(
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "50") int pageSize,
            Authentication auth) {
        User user = getCurrentUser(auth);
        Map<String, Object> messages = chatMessageService.getMessages(
            sessionId, user.getId(), pageNum, pageSize);
        return Result.success(messages);
    }

    /**
     * 患者端 - 发送消息
     */
    @PostMapping("/patient/chat/send")
    public Result<Map<String, Object>> patientSendMessage(
            @RequestBody Map<String, Object> params,
            Authentication auth) {
        User patient = getCurrentUser(auth);

        Long sessionId = Long.valueOf(params.get("sessionId").toString());
        Long receiverId = Long.valueOf(params.get("targetUserId").toString());
        String content = (String) params.get("content");
        String type = (String) params.getOrDefault("type", "text");

        Map<String, Object> message = chatMessageService.sendMessage(
            sessionId, patient.getId(), receiverId, content, type);

        return Result.success(message);
    }

    /**
     * 患者端 - 标记已读
     */
    @PostMapping("/patient/chat/read/{sessionId}")
    public Result<String> patientMarkRead(
            @PathVariable Long sessionId,
            Authentication auth) {
        User user = getCurrentUser(auth);
        chatMessageService.markAsRead(sessionId, user.getId());
        return Result.success("已标记为已读");
    }

    /**
     * 医生端 - 获取聊天列表
     */
    @GetMapping("/doctor/chat/list")
    public Result<List<Map<String, Object>>> getDoctorChatList(Authentication auth) {
        User user = getCurrentUser(auth);
        List<Map<String, Object>> list = chatMessageService.getChatList(user.getId(), "DOCTOR");
        return Result.success(list);
    }

    /**
     * 医生端 - 获取消息历史
     */
    @GetMapping("/doctor/chat/messages/{sessionId}")
    public Result<Map<String, Object>> getDoctorMessages(
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "50") int pageSize,
            Authentication auth) {
        User user = getCurrentUser(auth);
        Map<String, Object> messages = chatMessageService.getMessages(
            sessionId, user.getId(), pageNum, pageSize);
        return Result.success(messages);
    }

    /**
     * 医生端 - 发送消息
     */
    @PostMapping("/doctor/chat/send")
    public Result<Map<String, Object>> doctorSendMessage(
            @RequestBody Map<String, Object> params,
            Authentication auth) {
        User doctor = getCurrentUser(auth);

        Long sessionId = Long.valueOf(params.get("sessionId").toString());
        Long receiverId = Long.valueOf(params.get("targetUserId").toString());
        String content = (String) params.get("content");
        String type = (String) params.getOrDefault("type", "text");

        Map<String, Object> message = chatMessageService.sendMessage(
            sessionId, doctor.getId(), receiverId, content, type);

        return Result.success(message);
    }

    /**
     * 医生端 - 标记已读
     */
    @PostMapping("/doctor/chat/read/{sessionId}")
    public Result<String> doctorMarkRead(
            @PathVariable Long sessionId,
            Authentication auth) {
        User user = getCurrentUser(auth);
        chatMessageService.markAsRead(sessionId, user.getId());
        return Result.success("已标记为已读");
    }

    /**
     * 上传图片
     */
    @PostMapping("/chat/upload")
    public Result<Map<String, Object>> uploadImage(
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        try {
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            String uploadDir = System.getProperty("user.dir") + "/uploads/chat/";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File dest = new File(uploadDir + fileName);
            file.transferTo(dest);

            Map<String, Object> result = new HashMap<>();
            result.put("url", "/uploads/chat/" + fileName);
            return Result.success(result);
        } catch (IOException e) {
            return Result.error("上传失败: " + e.getMessage());
        }
    }
}
