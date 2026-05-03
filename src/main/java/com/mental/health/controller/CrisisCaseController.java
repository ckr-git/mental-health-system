package com.mental.health.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mental.health.common.JwtUtil;
import com.mental.health.common.Result;
import com.mental.health.entity.*;
import com.mental.health.service.CrisisCaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 危机案例管理Controller
 */
@RestController
@RequestMapping("/api")
public class CrisisCaseController {

    @Autowired
    private CrisisCaseService crisisCaseService;

    @Autowired
    private JwtUtil jwtUtil;

    // ===== 医生端 =====

    @GetMapping("/doctor/crisis-cases")
    public Result<Page<CrisisCase>> getDoctorCases(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        Long doctorId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        return Result.success(crisisCaseService.getDoctorCases(doctorId, status, pageNum, pageSize));
    }

    @GetMapping("/doctor/crisis-cases/{caseId}")
    public Result<CrisisCase> getCaseDetail(@PathVariable Long caseId) {
        CrisisCase c = crisisCaseService.getCaseDetail(caseId);
        return c != null ? Result.success(c) : Result.error("案例不存在");
    }

    @GetMapping("/doctor/crisis-cases/{caseId}/escalation-steps")
    public Result<List<CrisisEscalationStep>> getEscalationSteps(@PathVariable Long caseId) {
        return Result.success(crisisCaseService.getCaseEscalationSteps(caseId));
    }

    @GetMapping("/doctor/crisis-cases/{caseId}/contact-attempts")
    public Result<List<CrisisContactAttempt>> getContactAttempts(@PathVariable Long caseId) {
        return Result.success(crisisCaseService.getCaseContactAttempts(caseId));
    }

    @PostMapping("/doctor/crisis-cases/{caseId}/triage")
    public Result<Void> triageCase(
            @RequestHeader("Authorization") String token,
            @PathVariable Long caseId,
            @RequestBody Map<String, Object> body) {
        Long operatorId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        String triageLevel = (String) body.get("triageLevel");
        Long assignDoctorId = body.get("assignDoctorId") != null ? Long.valueOf(body.get("assignDoctorId").toString()) : null;
        String note = (String) body.get("note");
        crisisCaseService.triageCase(caseId, operatorId, triageLevel, assignDoctorId, note);
        return Result.success();
    }

    @PostMapping("/doctor/crisis-cases/{caseId}/transition")
    public Result<Void> transitionStatus(
            @RequestHeader("Authorization") String token,
            @PathVariable Long caseId,
            @RequestBody Map<String, String> body) {
        Long operatorId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        crisisCaseService.transitionStatus(caseId, body.get("targetStatus"), operatorId, body.get("note"));
        return Result.success();
    }

    @PostMapping("/doctor/crisis-cases/{caseId}/contact-attempts")
    public Result<Void> recordContactAttempt(
            @RequestHeader("Authorization") String token,
            @PathVariable Long caseId,
            @RequestBody Map<String, String> body) {
        Long operatorId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        crisisCaseService.recordContactAttempt(caseId, body.get("target"), body.get("channel"),
                body.get("contactName"), body.get("contactInfo"), body.get("status"), operatorId, body.get("note"));
        return Result.success();
    }

    // ===== 安全计划 =====

    @GetMapping("/doctor/patients/{patientId}/safety-plan")
    public Result<PatientSafetyPlan> getSafetyPlan(@PathVariable Long patientId) {
        PatientSafetyPlan plan = crisisCaseService.getActiveSafetyPlan(patientId);
        return plan != null ? Result.success(plan) : Result.success(null);
    }

    @PostMapping("/doctor/patients/{patientId}/safety-plan")
    public Result<Long> createSafetyPlan(
            @RequestHeader("Authorization") String token,
            @PathVariable Long patientId,
            @RequestBody PatientSafetyPlan plan) {
        Long doctorId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        return Result.success(crisisCaseService.createSafetyPlan(patientId, doctorId, plan));
    }

    // ===== 患者端 =====

    @GetMapping("/patient/crisis-cases")
    public Result<Page<CrisisCase>> getPatientCases(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        Long patientId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        return Result.success(crisisCaseService.getPatientCases(patientId, pageNum, pageSize));
    }

    @GetMapping("/patient/safety-plan")
    public Result<PatientSafetyPlan> getMysSafetyPlan(@RequestHeader("Authorization") String token) {
        Long patientId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        PatientSafetyPlan plan = crisisCaseService.getActiveSafetyPlan(patientId);
        return plan != null ? Result.success(plan) : Result.success(null);
    }
}
