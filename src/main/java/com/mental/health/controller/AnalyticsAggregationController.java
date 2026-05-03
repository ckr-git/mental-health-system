package com.mental.health.controller;

import com.mental.health.common.JwtUtil;
import com.mental.health.common.Result;
import com.mental.health.entity.*;
import com.mental.health.service.AnalyticsAggregationService;
import com.mental.health.service.MoodInsightService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AnalyticsAggregationController {

    @Autowired private AnalyticsAggregationService aggregationService;
    @Autowired private MoodInsightService moodInsightService;
    @Autowired private JwtUtil jwtUtil;

    // ===== 患者轨迹(医生端) =====
    @GetMapping("/doctor/patients/{patientId}/trajectory")
    public Result<List<PatientDailyMetric>> getTrajectory(
            @PathVariable Long patientId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return Result.success(aggregationService.getPatientTrajectory(patientId, from, to));
    }

    // ===== 医生工作负载(管理端) =====
    @GetMapping("/admin/analytics/doctors/{doctorId}/workload")
    public Result<List<DoctorWorkloadSnapshot>> getDoctorWorkload(
            @PathVariable Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return Result.success(aggregationService.getDoctorWorkloadTrend(doctorId, from, to));
    }

    // ===== 手动触发聚合(管理端) =====
    @PostMapping("/admin/analytics/jobs")
    public Result<Long> triggerJob(@RequestHeader("Authorization") String token,
            @RequestBody Map<String, String> body) {
        Long userId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        LocalDate targetDate = LocalDate.parse(body.get("targetDate"));
        return Result.success(aggregationService.triggerAggregation(
                body.get("jobScopeCode"), targetDate, userId));
    }

    // ===== 日记洞察(患者端) =====
    @GetMapping("/patient/mood-diaries/{diaryId}/insights")
    public Result<MoodDiaryFeature> getDiaryInsight(@PathVariable Long diaryId) {
        return Result.success(moodInsightService.getDiaryInsight(diaryId));
    }

    // ===== 日记洞察(医生端) =====
    @GetMapping("/doctor/patients/{patientId}/mood-insights/summary")
    public Result<Map<String, Object>> getMoodSummary(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "14") int days) {
        return Result.success(moodInsightService.summarizePatientMoodPattern(patientId, days));
    }

    @GetMapping("/doctor/patients/{patientId}/mood-insights/timeline")
    public Result<List<MoodDiaryFeature>> getMoodTimeline(
            @PathVariable Long patientId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return Result.success(moodInsightService.getPatientFeatureTimeline(patientId, from, to));
    }
}
