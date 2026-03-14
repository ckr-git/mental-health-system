package com.mental.health.controller;

import com.mental.health.common.JwtUtil;
import com.mental.health.common.Result;
import com.mental.health.entity.*;
import com.mental.health.service.TreatmentPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/doctor/treatment-plans")
@PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
public class TreatmentPlanController {

    @Autowired
    private TreatmentPlanService planService;

    @Autowired
    private JwtUtil jwtUtil;

    private Long extractUserId(String token) {
        return jwtUtil.getUserIdFromToken(token.substring(7));
    }

    // ===== Plans =====

    @GetMapping
    public Result<List<TreatmentPlan>> getMyPlans(
            @RequestParam(required = false) String status,
            @RequestHeader("Authorization") String token) {
        Long doctorId = extractUserId(token);
        return Result.success(planService.getDoctorPlans(doctorId, status));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> getPlanDetail(@PathVariable Long id) {
        Map<String, Object> detail = planService.getPlanDetail(id);
        if (detail == null) return Result.error("治疗计划不存在");
        return Result.success(detail);
    }

    @PostMapping
    public Result<TreatmentPlan> createPlan(@RequestBody TreatmentPlan plan,
                                             @RequestHeader("Authorization") String token) {
        Long doctorId = extractUserId(token);
        plan.setDoctorId(doctorId);
        return Result.success("创建成功", planService.createPlan(plan));
    }

    @PutMapping("/{id}")
    public Result<TreatmentPlan> updatePlan(@PathVariable Long id,
                                             @RequestBody TreatmentPlan plan) {
        plan.setId(id);
        return Result.success("更新成功", planService.updatePlan(plan));
    }

    @PostMapping("/{id}/activate")
    public Result<String> activatePlan(@PathVariable Long id,
                                       @RequestHeader("Authorization") String token) {
        Long doctorId = extractUserId(token);
        if (planService.activatePlan(id, doctorId)) {
            return Result.success("已激活");
        }
        return Result.error("激活失败");
    }

    @PostMapping("/{id}/complete")
    public Result<String> completePlan(@PathVariable Long id,
                                       @RequestHeader("Authorization") String token) {
        Long doctorId = extractUserId(token);
        if (planService.completePlan(id, doctorId)) {
            return Result.success("已完成");
        }
        return Result.error("完成失败");
    }

    // ===== Goals =====

    @PostMapping("/{planId}/goals")
    public Result<TreatmentGoal> addGoal(@PathVariable Long planId,
                                          @RequestBody TreatmentGoal goal) {
        goal.setPlanId(planId);
        return Result.success("创建成功", planService.addGoal(goal));
    }

    @PutMapping("/goals/{goalId}")
    public Result<TreatmentGoal> updateGoal(@PathVariable Long goalId,
                                             @RequestBody TreatmentGoal goal) {
        goal.setId(goalId);
        return Result.success("更新成功", planService.updateGoal(goal));
    }

    @PostMapping("/goals/{goalId}/progress")
    public Result<String> updateGoalProgress(@PathVariable Long goalId,
                                              @RequestBody Map<String, Object> body,
                                              @RequestHeader("Authorization") String token) {
        Long doctorId = extractUserId(token);
        int progressPct = (int) body.getOrDefault("progressPct", 0);
        String note = (String) body.get("note");
        if (planService.updateGoalProgress(goalId, doctorId, progressPct, note)) {
            return Result.success("已更新");
        }
        return Result.error("更新失败");
    }

    @GetMapping("/goals/{goalId}/progress-history")
    public Result<List<GoalProgressLog>> getGoalProgressHistory(@PathVariable Long goalId) {
        return Result.success(planService.getGoalProgressHistory(goalId));
    }

    // ===== Interventions =====

    @PostMapping("/{planId}/interventions")
    public Result<TreatmentIntervention> addIntervention(@PathVariable Long planId,
                                                          @RequestBody TreatmentIntervention intervention) {
        intervention.setPlanId(planId);
        return Result.success("创建成功", planService.addIntervention(intervention));
    }

    @PutMapping("/interventions/{interventionId}")
    public Result<TreatmentIntervention> updateIntervention(@PathVariable Long interventionId,
                                                             @RequestBody TreatmentIntervention intervention) {
        intervention.setId(interventionId);
        return Result.success("更新成功", planService.updateIntervention(intervention));
    }

    // ===== Session Notes =====

    @PostMapping("/session-notes")
    public Result<SessionNote> createSessionNote(@RequestBody SessionNote note,
                                                  @RequestHeader("Authorization") String token) {
        Long doctorId = extractUserId(token);
        note.setDoctorId(doctorId);
        return Result.success("创建成功", planService.createSessionNote(note));
    }

    @PutMapping("/session-notes/{noteId}")
    public Result<SessionNote> updateSessionNote(@PathVariable Long noteId,
                                                  @RequestBody SessionNote note) {
        note.setId(noteId);
        return Result.success("更新成功", planService.updateSessionNote(note));
    }

    @GetMapping("/patients/{patientId}/session-notes")
    public Result<List<SessionNote>> getPatientSessionNotes(@PathVariable Long patientId) {
        return Result.success(planService.getPatientSessionNotes(patientId));
    }

    @GetMapping("/session-notes/{noteId}")
    public Result<SessionNote> getSessionNote(@PathVariable Long noteId) {
        SessionNote note = planService.getSessionNote(noteId);
        if (note == null) return Result.error("记录不存在");
        return Result.success(note);
    }
}
