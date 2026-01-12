package com.mental.health.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mental.health.common.JwtUtil;
import com.mental.health.common.Result;
import com.mental.health.entity.AssessmentReport;
import com.mental.health.service.AssessmentReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评估报告控制器
 */
@RestController
@RequestMapping("/api")
public class AssessmentReportController {

    @Autowired
    private AssessmentReportService assessmentReportService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 获取我的评估报告列表（患者）
     */
    @GetMapping("/patient/reports")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    public Result<IPage<AssessmentReport>> getMyReports(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        IPage<AssessmentReport> reports = assessmentReportService.getUserReports(userId, pageNum, pageSize);
        return Result.success(reports);
    }

    /**
     * 获取评估报告详情
     */
    @GetMapping("/patient/reports/{id}")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    public Result<AssessmentReport> getReportDetail(@PathVariable Long id) {
        AssessmentReport report = assessmentReportService.getReportById(id);
        if (report != null) {
            return Result.success(report);
        } else {
            return Result.error("报告不存在");
        }
    }

    /**
     * 获取最近的报告
     */
    @GetMapping("/patient/reports/recent")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    public Result<List<AssessmentReport>> getRecentReports(
            @RequestParam(defaultValue = "5") int limit,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        List<AssessmentReport> reports = assessmentReportService.getRecentReports(userId, limit);
        return Result.success(reports);
    }

    /**
     * 创建评估报告（医生）
     */
    @PostMapping("/doctor/reports/create")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public Result<String> createReport(@RequestBody AssessmentReport report, @RequestHeader("Authorization") String token) {
        Long doctorId = jwtUtil.getUserIdFromToken(token.substring(7));
        report.setDoctorId(doctorId);
        report.setIsAiGenerated(0);
        
        boolean success = assessmentReportService.createReport(report);
        if (success) {
            return Result.success("创建成功");
        } else {
            return Result.error("创建失败");
        }
    }

    /**
     * 获取医生创建的报告列表
     */
    @GetMapping("/doctor/reports/list")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public Result<IPage<AssessmentReport>> getDoctorReports(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestHeader("Authorization") String token) {
        Long doctorId = jwtUtil.getUserIdFromToken(token.substring(7));
        IPage<AssessmentReport> reports = assessmentReportService.getDoctorReports(doctorId, pageNum, pageSize);
        return Result.success(reports);
    }

    /**
     * 更新评估报告（医生）
     */
    @PutMapping("/doctor/reports/update")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public Result<String> updateReport(@RequestBody AssessmentReport report) {
        boolean success = assessmentReportService.updateReport(report);
        if (success) {
            return Result.success("更新成功");
        } else {
            return Result.error("更新失败");
        }
    }

    // DELETE接口已移至DoctorController，避免路径冲突
}
