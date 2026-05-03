package com.mental.health.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mental.health.common.JwtUtil;
import com.mental.health.common.Result;
import com.mental.health.entity.*;
import com.mental.health.service.AssessmentAssignmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AssessmentAssignmentController {

    @Autowired private AssessmentAssignmentService assignmentService;
    @Autowired private JwtUtil jwtUtil;

    // ===== 患者端 =====

    @GetMapping("/patient/assessment-assignments/pending")
    public Result<List<AssessmentAssignment>> getMyPendingAssignments(
            @RequestHeader("Authorization") String token) {
        Long patientId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        return Result.success(assignmentService.getPatientPendingAssignments(patientId));
    }

    @GetMapping("/patient/assessment-assignments")
    public Result<IPage<AssessmentAssignment>> getMyAssignmentHistory(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        Long patientId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        return Result.success(assignmentService.getPatientAssignmentHistory(patientId, pageNum, pageSize));
    }

    @PostMapping("/patient/assessment-assignments/{assignmentId}/link-session")
    public Result<Void> linkSession(
            @PathVariable Long assignmentId,
            @RequestBody Map<String, Long> body) {
        assignmentService.linkSessionToAssignment(assignmentId, body.get("sessionId"));
        return Result.success();
    }

    // ===== 医生端 =====

    @PostMapping("/doctor/patients/{patientId}/assessment-assignments")
    public Result<Long> assignAssessment(
            @RequestHeader("Authorization") String token,
            @PathVariable Long patientId,
            @RequestBody Map<String, Object> body) {
        Long doctorId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        String scaleCode = (String) body.get("scaleCode");
        boolean isBaseline = Boolean.TRUE.equals(body.get("isBaseline"));
        Long treatmentPlanId = body.get("treatmentPlanId") != null
                ? Long.valueOf(body.get("treatmentPlanId").toString()) : null;
        LocalDateTime dueAt = body.get("dueAt") != null
                ? LocalDateTime.parse(body.get("dueAt").toString()) : null;

        Long id = assignmentService.assignManually(patientId, scaleCode, doctorId,
                dueAt, isBaseline, treatmentPlanId);
        return Result.success(id);
    }

    @PostMapping("/doctor/assessment-assignments/{assignmentId}/review")
    public Result<Void> reviewAssignment(
            @RequestHeader("Authorization") String token,
            @PathVariable Long assignmentId,
            @RequestBody Map<String, String> body) {
        Long doctorId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        assignmentService.reviewAssignment(assignmentId, doctorId, body.get("note"));
        return Result.success();
    }

    @GetMapping("/doctor/patients/{patientId}/assessment-assignments")
    public Result<IPage<AssessmentAssignment>> getPatientAssignments(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(assignmentService.getPatientAssignmentHistory(patientId, pageNum, pageSize));
    }

    @GetMapping("/doctor/patients/{patientId}/assessment-baseline")
    public Result<AssessmentBaseline> getBaseline(
            @PathVariable Long patientId,
            @RequestParam String scaleCode) {
        return Result.success(assignmentService.getActiveBaseline(patientId, scaleCode));
    }

    @GetMapping("/doctor/patients/{patientId}/assessment-dimension-trend")
    public Result<List<AssessmentDimensionResult>> getDimensionTrend(
            @PathVariable Long patientId,
            @RequestParam String scaleCode,
            @RequestParam String dimensionCode,
            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(assignmentService.getDimensionTrend(patientId, scaleCode, dimensionCode, limit));
    }

    // ===== 协议触发 =====

    @PostMapping("/doctor/patients/{patientId}/trigger-protocol")
    public Result<List<Long>> triggerProtocol(
            @RequestHeader("Authorization") String token,
            @PathVariable Long patientId,
            @RequestBody Map<String, Object> body) {
        Long doctorId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        String trigger = (String) body.get("triggerCondition");
        String sourceType = (String) body.get("sourceEventType");
        Long sourceId = body.get("sourceEventId") != null
                ? Long.valueOf(body.get("sourceEventId").toString()) : null;
        return Result.success(assignmentService.triggerProtocol(trigger, patientId, doctorId, sourceType, sourceId));
    }
}
