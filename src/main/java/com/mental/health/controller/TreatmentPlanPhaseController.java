package com.mental.health.controller;

import com.mental.health.common.JwtUtil;
import com.mental.health.common.Result;
import com.mental.health.entity.*;
import com.mental.health.service.TreatmentPlanPhaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/doctor")
public class TreatmentPlanPhaseController {

    @Autowired private TreatmentPlanPhaseService phaseService;
    @Autowired private JwtUtil jwtUtil;

    // ===== 阶段 =====
    @GetMapping("/treatment-plans/{planId}/phases")
    public Result<List<TreatmentPlanPhase>> getPhases(@PathVariable Long planId) {
        return Result.success(phaseService.getPlanPhases(planId));
    }

    @PostMapping("/treatment-plans/{planId}/phases/init")
    public Result<Void> initPhases(@PathVariable Long planId) {
        phaseService.initDefaultPhases(planId);
        return Result.success();
    }

    @PostMapping("/treatment-plans/{planId}/phases/{phaseId}/activate")
    public Result<Void> activatePhase(@RequestHeader("Authorization") String token,
            @PathVariable Long planId, @PathVariable Long phaseId) {
        Long doctorId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        phaseService.activatePhase(planId, phaseId, doctorId);
        return Result.success();
    }

    @PostMapping("/treatment-plans/{planId}/phases/{phaseId}/complete")
    public Result<Void> completePhase(@RequestHeader("Authorization") String token,
            @PathVariable Long planId, @PathVariable Long phaseId, @RequestBody Map<String, String> body) {
        Long doctorId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        phaseService.completePhase(planId, phaseId, doctorId, body.get("note"));
        return Result.success();
    }

    // ===== 评审 =====
    @GetMapping("/treatment-plans/{planId}/reviews")
    public Result<List<TreatmentPlanReview>> getReviews(@PathVariable Long planId,
            @RequestParam(required = false) String status) {
        return Result.success(phaseService.getPlanReviews(planId, status));
    }

    @PostMapping("/treatment-plans/{planId}/reviews")
    public Result<Long> createReview(@PathVariable Long planId, @RequestBody Map<String, Object> body) {
        Long phaseId = body.get("phaseId") != null ? Long.valueOf(body.get("phaseId").toString()) : null;
        LocalDateTime dueAt = body.get("dueAt") != null ? LocalDateTime.parse(body.get("dueAt").toString()) : LocalDateTime.now().plusDays(14);
        return Result.success(phaseService.createScheduledReview(planId, phaseId, dueAt));
    }

    @PostMapping("/treatment-plan-reviews/{reviewId}/complete")
    public Result<Void> completeReview(@RequestHeader("Authorization") String token,
            @PathVariable Long reviewId, @RequestBody Map<String, String> body) {
        Long doctorId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        phaseService.completeReview(reviewId, doctorId, body.get("conclusionCode"),
                body.get("summaryText"), body.get("actionPlanJson"));
        return Result.success();
    }

    // ===== 修订 =====
    @PostMapping("/treatment-plans/{planId}/revisions")
    public Result<Long> proposeRevision(@RequestHeader("Authorization") String token,
            @PathVariable Long planId, @RequestBody Map<String, String> body) {
        Long doctorId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        Long reviewId = body.get("reviewId") != null ? Long.valueOf(body.get("reviewId")) : null;
        return Result.success(phaseService.proposeRevision(planId, doctorId,
                body.get("changeSummary"), body.get("changeDetailJson"), body.get("reason"), reviewId));
    }

    @PostMapping("/treatment-plan-revisions/{revisionId}/approve")
    public Result<Void> approveRevision(@RequestHeader("Authorization") String token,
            @PathVariable Long revisionId) {
        Long doctorId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        phaseService.approveAndApplyRevision(revisionId, doctorId);
        return Result.success();
    }

    // ===== 干预任务 =====
    @PostMapping("/patients/{patientId}/intervention-tasks")
    public Result<Long> assignTask(@RequestHeader("Authorization") String token,
            @PathVariable Long patientId, @RequestBody Map<String, Object> body) {
        Long doctorId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        Long planId = Long.valueOf(body.get("planId").toString());
        Long phaseId = body.get("phaseId") != null ? Long.valueOf(body.get("phaseId").toString()) : null;
        Long interventionId = body.get("interventionId") != null ? Long.valueOf(body.get("interventionId").toString()) : null;
        LocalDateTime dueAt = body.get("dueAt") != null ? LocalDateTime.parse(body.get("dueAt").toString()) : null;
        return Result.success(phaseService.assignTask(planId, phaseId, interventionId, patientId, doctorId,
                (String) body.get("taskType"), (String) body.get("title"), (String) body.get("description"), dueAt));
    }

    @PostMapping("/intervention-tasks/{taskId}/review")
    public Result<Void> reviewTask(@RequestHeader("Authorization") String token,
            @PathVariable Long taskId, @RequestBody Map<String, String> body) {
        Long doctorId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        phaseService.reviewTask(taskId, doctorId, body.get("resultCode"), body.get("note"));
        return Result.success();
    }

    @GetMapping("/treatment-plans/{planId}/tasks")
    public Result<List<InterventionTask>> getPlanTasks(@PathVariable Long planId) {
        return Result.success(phaseService.getPlanTasks(planId));
    }
}
