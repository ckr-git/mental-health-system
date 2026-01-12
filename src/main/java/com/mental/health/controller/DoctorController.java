package com.mental.health.controller;

import com.mental.health.common.JwtUtil;
import com.mental.health.common.Result;
import com.mental.health.entity.AssessmentReport;
import com.mental.health.entity.User;
import com.mental.health.service.ConsultationService;
import com.mental.health.service.DoctorService;
import com.mental.health.service.PatientDoctorRelationshipService;
import com.mental.health.service.RelationshipChangeRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 医生控制器
 */
@RestController
@RequestMapping("/api/doctor")
@PreAuthorize("hasRole('DOCTOR')")
public class DoctorController {

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RelationshipChangeRequestService relationshipChangeRequestService;

    @Autowired
    private ConsultationService consultationService;

    @Autowired
    private PatientDoctorRelationshipService patientDoctorRelationshipService;

    /**
     * Get doctor dashboard statistics
     */
    @GetMapping("/dashboard/statistics")
    public Result<Map<String, Object>> getDashboardStatistics(Authentication authentication) {
        String username = authentication.getName();
        Map<String, Object> statistics = doctorService.getDashboardStatistics(username);
        return Result.success(statistics);
    }

    /**
     * Get doctor's patients list with pagination
     */
    @GetMapping("/patients")
    public Result<Map<String, Object>> getPatients(
            Authentication authentication,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        String username = authentication.getName();
        Map<String, Object> result = doctorService.getPatients(username, pageNum, pageSize, keyword);
        return Result.success(result);
    }

    /**
     * Get patient detail
     */
    @GetMapping("/patients/{patientId}")
    public Result<Map<String, Object>> getPatientDetail(
            Authentication authentication,
            @PathVariable Long patientId) {
        String username = authentication.getName();
        Map<String, Object> detail = doctorService.getPatientDetail(username, patientId);
        if (detail != null) {
            return Result.success(detail);
        } else {
            return Result.error("患者不存在或无权限查看");
        }
    }

    /**
     * Get doctor's assessment reports
     */
    @GetMapping("/reports")
    public Result<Map<String, Object>> getReports(
            Authentication authentication,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long patientId) {
        String username = authentication.getName();
        Map<String, Object> result = doctorService.getReports(username, pageNum, pageSize, patientId);
        return Result.success(result);
    }

    /**
     * Get report detail
     */
    @GetMapping("/reports/{reportId}")
    public Result<AssessmentReport> getReportDetail(
            Authentication authentication,
            @PathVariable Long reportId) {
        String username = authentication.getName();
        AssessmentReport report = doctorService.getReportDetail(username, reportId);
        if (report != null) {
            return Result.success(report);
        } else {
            return Result.error("报告不存在或无权限查看");
        }
    }

    /**
     * Create assessment report
     */
    @PostMapping("/reports")
    public Result<String> createReport(
            Authentication authentication,
            @RequestBody Map<String, Object> requestData) {
        String username = authentication.getName();

        // Map patientId to userId in AssessmentReport
        AssessmentReport report = new AssessmentReport();
        if (requestData.containsKey("patientId")) {
            report.setUserId(Long.valueOf(requestData.get("patientId").toString()));
        }
        if (requestData.containsKey("reportType")) {
            report.setReportType(requestData.get("reportType").toString());
        }
        if (requestData.containsKey("summary")) {
            report.setSummary(requestData.get("summary").toString());
        }
        if (requestData.containsKey("content")) {
            report.setContent(requestData.get("content").toString());
        }
        if (requestData.containsKey("recommendations")) {
            report.setSuggestions(requestData.get("recommendations").toString());
        }

        boolean success = doctorService.createReport(username, report);
        if (success) {
            return Result.success("创建成功");
        } else {
            return Result.error("创建失败");
        }
    }

    /**
     * Update assessment report
     */
    @PutMapping("/reports/{reportId}")
    public Result<String> updateReport(
            Authentication authentication,
            @PathVariable Long reportId,
            @RequestBody Map<String, Object> requestData) {
        String username = authentication.getName();

        // Map patientId to userId in AssessmentReport
        AssessmentReport report = new AssessmentReport();
        report.setId(reportId);
        if (requestData.containsKey("patientId")) {
            report.setUserId(Long.valueOf(requestData.get("patientId").toString()));
        }
        if (requestData.containsKey("reportType")) {
            report.setReportType(requestData.get("reportType").toString());
        }
        if (requestData.containsKey("summary")) {
            report.setSummary(requestData.get("summary").toString());
        }
        if (requestData.containsKey("content")) {
            report.setContent(requestData.get("content").toString());
        }
        if (requestData.containsKey("recommendations")) {
            report.setSuggestions(requestData.get("recommendations").toString());
        }

        boolean success = doctorService.updateReport(username, report);
        if (success) {
            return Result.success("更新成功");
        } else {
            return Result.error("更新失败或无权限");
        }
    }

    /**
     * Delete assessment report
     */
    @DeleteMapping("/reports/{reportId}")
    public Result<String> deleteReport(
            Authentication authentication,
            @PathVariable Long reportId) {
        String username = authentication.getName();
        boolean success = doctorService.deleteReport(username, reportId);
        if (success) {
            return Result.success("删除成功");
        } else {
            return Result.error("删除失败或无权限");
        }
    }

    /**
     * Get doctor's appointments
     */
    @GetMapping("/appointments")
    public Result<Map<String, Object>> getAppointments(
            Authentication authentication,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer status) {
        String username = authentication.getName();
        Map<String, Object> result = doctorService.getAppointments(username, pageNum, pageSize, status);
        return Result.success(result);
    }

    /**
     * Get recent appointments (for dashboard)
     */
    @GetMapping("/appointments/recent")
    public Result<List<Map<String, Object>>> getRecentAppointments(Authentication authentication) {
        String username = authentication.getName();
        List<Map<String, Object>> appointments = doctorService.getRecentAppointments(username);
        return Result.success(appointments);
    }

    // ==================== 患者公海相关 ====================

    /**
     * 获取患者公海列表（未分配医生的患者）
     */
    @GetMapping("/patient-pool")
    public Result<Map<String, Object>> getPatientPool(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        Long doctorId = jwtUtil.getUserIdFromToken(token.substring(7));

        // 获取未分配的患者列表
        List<Map<String, Object>> allUnassigned = patientDoctorRelationshipService.getUnassignedPatients();

        // 如果有关键词搜索,进行过滤
        if (keyword != null && !keyword.trim().isEmpty()) {
            allUnassigned = allUnassigned.stream()
                .filter(patient -> {
                    String nickname = (String) patient.get("nickname");
                    String username = (String) patient.get("username");
                    return (nickname != null && nickname.contains(keyword)) ||
                           (username != null && username.contains(keyword));
                })
                .collect(java.util.stream.Collectors.toList());
        }

        // 手动分页
        int total = allUnassigned.size();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);

        List<Map<String, Object>> pagedList;
        if (fromIndex < total) {
            pagedList = allUnassigned.subList(fromIndex, toIndex);
        } else {
            pagedList = new java.util.ArrayList<>();
        }

        // 构造分页结果
        Map<String, Object> result = new HashMap<>();
        result.put("records", pagedList);
        result.put("total", total);
        result.put("size", pageSize);
        result.put("current", pageNum);
        result.put("pages", (int) Math.ceil((double) total / pageSize));

        return Result.success(result);
    }

    /**
     * 认领患者（提交审批请求）
     */
    @PostMapping("/patient-pool/claim/{patientId}")
    public Result<String> claimPatient(
            @RequestHeader("Authorization") String token,
            @PathVariable Long patientId,
            @RequestBody Map<String, String> request) {
        Long doctorId = jwtUtil.getUserIdFromToken(token.substring(7));
        String reason = request.get("reason");

        if (reason == null || reason.trim().isEmpty()) {
            return Result.error("请填写认领理由");
        }

        try {
            relationshipChangeRequestService.submitClaimRequest(doctorId, patientId, reason);
            return Result.success("认领申请已提交，等待管理员审核");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // ==================== 患者管理相关 ====================

    /**
     * 释放患者（提交审批请求）
     */
    @PostMapping("/patients/{patientId}/release")
    public Result<String> releasePatient(
            @RequestHeader("Authorization") String token,
            @PathVariable Long patientId,
            @RequestBody Map<String, String> request) {
        Long doctorId = jwtUtil.getUserIdFromToken(token.substring(7));
        String reason = request.get("reason");

        if (reason == null || reason.trim().isEmpty()) {
            return Result.error("请填写释放理由");
        }

        try {
            relationshipChangeRequestService.submitReleaseRequest(doctorId, patientId, reason);
            return Result.success("释放申请已提交，等待管理员审核");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // ==================== 认领/释放记录 ====================

    /**
     * 获取医生的认领/释放申请记录
     */
    @GetMapping("/requests")
    public Result<Map<String, Object>> getDoctorRequests(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String requestType,
            @RequestParam(required = false) Integer status) {
        Long doctorId = jwtUtil.getUserIdFromToken(token.substring(7));
        Map<String, Object> result = relationshipChangeRequestService.getDoctorRequests(doctorId, pageNum, pageSize);
        return Result.success(result);
    }

    // ==================== 在线咨询相关 ====================

    /**
     * 获取医生的咨询会话列表
     */
    @GetMapping("/consultations")
    public Result<Map<String, Object>> getConsultations(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer status) {
        Long doctorId = jwtUtil.getUserIdFromToken(token.substring(7));
        Map<String, Object> result = consultationService.getDoctorSessions(doctorId, pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 获取会话详情
     */
    @GetMapping("/consultations/{sessionId}")
    public Result<Map<String, Object>> getConsultationDetail(
            @RequestHeader("Authorization") String token,
            @PathVariable Long sessionId) {
        Long doctorId = jwtUtil.getUserIdFromToken(token.substring(7));
        // TODO: Add permission check for doctor accessing this session
        return Result.error("会话详情功能暂未实现");
    }

    /**
     * 关闭会话
     */
    @PostMapping("/consultations/{sessionId}/close")
    public Result<String> closeConsultation(
            @RequestHeader("Authorization") String token,
            @PathVariable Long sessionId) {
        Long doctorId = jwtUtil.getUserIdFromToken(token.substring(7));
        try {
            consultationService.closeSession(sessionId);
            return Result.success("会话已关闭");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
