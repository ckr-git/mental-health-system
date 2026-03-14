package com.mental.health.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mental.health.common.JwtUtil;
import com.mental.health.common.Result;
import com.mental.health.dto.*;
import com.mental.health.service.AssessmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AssessmentController {

    @Autowired
    private AssessmentService assessmentService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/patient/assessments/scales")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    public Result<List<ScaleListItem>> getAvailableScales() {
        return Result.success(assessmentService.getAvailableScales());
    }

    @GetMapping("/patient/assessments/scales/{scaleCode}")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    public Result<ScaleDetailDTO> getScaleDetail(@PathVariable String scaleCode) {
        ScaleDetailDTO detail = assessmentService.getScaleDetail(scaleCode);
        if (detail == null) {
            return Result.error("评估量表不存在");
        }
        return Result.success(detail);
    }

    @PostMapping("/patient/assessments/sessions")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    public Result<Long> startSession(@RequestBody StartSessionRequest request,
                                     @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        Long sessionId = assessmentService.startSession(userId, request.getScaleCode());
        return Result.success(sessionId);
    }

    @PutMapping("/patient/assessments/sessions/{id}/answers")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    public Result<Void> saveAnswers(@PathVariable Long id,
                                    @RequestBody SaveAnswersRequest request,
                                    @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        assessmentService.saveAnswers(userId, id, request.getAnswers());
        return Result.success(null);
    }

    @PostMapping("/patient/assessments/sessions/{id}/submit")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    public Result<SessionResultDTO> submitSession(@PathVariable Long id,
                                                  @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        SessionResultDTO result = assessmentService.submitSession(userId, id);
        return Result.success(result);
    }

    @GetMapping("/patient/assessments/history")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    public Result<IPage<AssessmentHistoryItem>> getHistory(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        return Result.success(assessmentService.getHistory(userId, pageNum, pageSize));
    }

    @GetMapping("/doctor/patients/{patientId}/assessments")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public Result<IPage<AssessmentHistoryItem>> getPatientAssessments(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(assessmentService.getPatientAssessments(patientId, pageNum, pageSize));
    }
}
