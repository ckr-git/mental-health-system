package com.mental.health.controller;

import com.mental.health.common.JwtUtil;
import com.mental.health.common.Result;
import com.mental.health.entity.OutcomeSnapshot;
import com.mental.health.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private JwtUtil jwtUtil;

    private Long extractUserId(String token) {
        return jwtUtil.getUserIdFromToken(token.substring(7));
    }

    // Patient: my outcome summary
    @GetMapping("/patient/outcomes/summary")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    public Result<Map<String, Object>> getMyOutcomeSummary(@RequestHeader("Authorization") String token) {
        Long patientId = extractUserId(token);
        return Result.success(analyticsService.getPatientOutcomeSummary(patientId));
    }

    // Patient: my outcome history
    @GetMapping("/patient/outcomes/history")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    public Result<List<OutcomeSnapshot>> getMyOutcomeHistory(@RequestHeader("Authorization") String token) {
        Long patientId = extractUserId(token);
        return Result.success(analyticsService.getPatientOutcomeHistory(patientId));
    }

    // Doctor: patient outcome summary
    @GetMapping("/doctor/patients/{patientId}/outcomes")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public Result<Map<String, Object>> getPatientOutcome(@PathVariable Long patientId) {
        return Result.success(analyticsService.getPatientOutcomeSummary(patientId));
    }

    // Doctor: generate outcome snapshot
    @PostMapping("/doctor/patients/{patientId}/outcomes/snapshot")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public Result<OutcomeSnapshot> generateSnapshot(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "MONTHLY") String snapshotType,
            @RequestHeader("Authorization") String token) {
        Long doctorId = extractUserId(token);
        OutcomeSnapshot snapshot = analyticsService.generateSnapshot(patientId, doctorId, snapshotType);
        return Result.success("快照已生成", snapshot);
    }

    // Doctor: patient outcome history
    @GetMapping("/doctor/patients/{patientId}/outcomes/history")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public Result<List<OutcomeSnapshot>> getPatientOutcomeHistory(@PathVariable Long patientId) {
        return Result.success(analyticsService.getPatientOutcomeHistory(patientId));
    }

    // Doctor: my performance
    @GetMapping("/doctor/performance")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public Result<Map<String, Object>> getDoctorPerformance(@RequestHeader("Authorization") String token) {
        Long doctorId = extractUserId(token);
        return Result.success(analyticsService.getDoctorPerformanceSummary(doctorId));
    }

    // Admin: system analytics
    @GetMapping("/admin/analytics")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getSystemAnalytics() {
        return Result.success(analyticsService.getSystemAnalytics());
    }
}
