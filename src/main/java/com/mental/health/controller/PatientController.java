package com.mental.health.controller;

import com.mental.health.common.JwtUtil;
import com.mental.health.common.Result;
import com.mental.health.service.ConsultationService;
import com.mental.health.service.PatientDoctorRelationshipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 患者控制器
 */
@RestController
@RequestMapping("/api/patient")
@PreAuthorize("hasRole('PATIENT')")
public class PatientController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PatientDoctorRelationshipService patientDoctorRelationshipService;

    @Autowired
    private ConsultationService consultationService;

    // ==================== 我的医生 ====================

    /**
     * 获取我的医生信息
     */
    @GetMapping("/my-doctor")
    public Result<Map<String, Object>> getMyDoctor(
            @RequestHeader("Authorization") String token) {
        Long patientId = jwtUtil.getUserIdFromToken(token.substring(7));
        Map<String, Object> doctor = patientDoctorRelationshipService.getPatientDoctor(patientId);

        if (doctor != null) {
            return Result.success(doctor);
        } else {
            return Result.success("您还没有分配医生", null);
        }
    }

    /**
     * 获取我的当前咨询会话
     */
    @GetMapping("/my-consultation")
    public Result<Map<String, Object>> getMyConsultation(
            @RequestHeader("Authorization") String token) {
        Long patientId = jwtUtil.getUserIdFromToken(token.substring(7));
        Map<String, Object> consultation = consultationService.getPatientSession(patientId);

        if (consultation != null) {
            return Result.success(consultation);
        } else {
            return Result.success("当前没有进行中的咨询会话", null);
        }
    }

    /**
     * 获取咨询历史记录
     */
    @GetMapping("/consultation-history")
    public Result<Map<String, Object>> getConsultationHistory(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long patientId = jwtUtil.getUserIdFromToken(token.substring(7));
        Map<String, Object> history = consultationService.getPatientConsultationHistory(patientId, pageNum, pageSize);
        return Result.success(history);
    }

    /**
     * 发起新的咨询会话
     */
    @PostMapping("/consultation/start")
    public Result<Map<String, Object>> startConsultation(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, String> request) {
        Long patientId = jwtUtil.getUserIdFromToken(token.substring(7));
        String initialMessage = request.get("message");

        if (initialMessage == null || initialMessage.trim().isEmpty()) {
            return Result.error("请输入咨询内容");
        }

        // 获取患者的医生
        Map<String, Object> doctorInfo = patientDoctorRelationshipService.getPatientDoctor(patientId);
        if (doctorInfo == null) {
            return Result.error("您还没有分配医生，无法发起咨询");
        }

        Long doctorId = (Long) doctorInfo.get("doctorId");
        Map<String, Object> session = consultationService.startConsultation(patientId, doctorId, initialMessage);
        return Result.success("咨询会话已创建", session);
    }
}
