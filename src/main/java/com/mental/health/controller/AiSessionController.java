package com.mental.health.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mental.health.common.JwtUtil;
import com.mental.health.common.Result;
import com.mental.health.entity.*;
import com.mental.health.service.AiSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AiSessionController {

    @Autowired private AiSessionService aiSessionService;
    @Autowired private JwtUtil jwtUtil;

    // ===== 患者端 =====
    @PostMapping("/patient/ai/sessions")
    public Result<Long> openSession(@RequestHeader("Authorization") String token,
            @RequestBody(required = false) Map<String, String> body) {
        Long patientId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        String sessionType = body != null ? body.get("sessionType") : null;
        return Result.success(aiSessionService.openSession(patientId, sessionType));
    }

    @GetMapping("/patient/ai/sessions")
    public Result<List<AiSession>> getSessions(@RequestHeader("Authorization") String token,
            @RequestParam(required = false) String status) {
        Long patientId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        return Result.success(aiSessionService.getPatientSessions(patientId, status));
    }

    @PostMapping("/patient/ai/sessions/{sessionId}/messages")
    public Result<Map<String, Object>> sendMessage(@RequestHeader("Authorization") String token,
            @PathVariable Long sessionId, @RequestBody Map<String, String> body) {
        Long patientId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        return Result.success(aiSessionService.sendMessage(sessionId, patientId, body.get("content")));
    }

    @GetMapping("/patient/ai/sessions/{sessionId}/messages")
    public Result<IPage<AiMessage>> getMessages(@PathVariable Long sessionId,
            @RequestParam(defaultValue = "1") int pageNum, @RequestParam(defaultValue = "50") int pageSize) {
        return Result.success(aiSessionService.getSessionMessages(sessionId, pageNum, pageSize));
    }

    @PostMapping("/patient/ai/sessions/{sessionId}/close")
    public Result<Void> closeSession(@RequestHeader("Authorization") String token,
            @PathVariable Long sessionId, @RequestBody(required = false) Map<String, String> body) {
        Long patientId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        aiSessionService.closeSession(sessionId, patientId, body != null ? body.get("reason") : null);
        return Result.success();
    }

    // ===== 医生端 =====
    @GetMapping("/doctor/ai-handoff-tasks")
    public Result<IPage<AiHandoffTask>> getHandoffTasks(@RequestHeader("Authorization") String token,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int pageNum, @RequestParam(defaultValue = "20") int pageSize) {
        Long doctorId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        return Result.success(aiSessionService.getDoctorHandoffTasks(doctorId, status, pageNum, pageSize));
    }

    @PostMapping("/doctor/ai-handoff-tasks/{taskId}/acknowledge")
    public Result<Void> ackHandoff(@RequestHeader("Authorization") String token, @PathVariable Long taskId) {
        Long doctorId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        aiSessionService.acknowledgeHandoff(taskId, doctorId);
        return Result.success();
    }

    @PostMapping("/doctor/ai-handoff-tasks/{taskId}/complete")
    public Result<Void> completeHandoff(@RequestHeader("Authorization") String token,
            @PathVariable Long taskId, @RequestBody Map<String, String> body) {
        Long doctorId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        aiSessionService.completeHandoff(taskId, doctorId, body.get("note"), body.get("followUpAction"));
        return Result.success();
    }
}
