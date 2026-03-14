package com.mental.health.controller;

import com.mental.health.common.JwtUtil;
import com.mental.health.common.Result;
import com.mental.health.entity.TreatmentPlan;
import com.mental.health.service.TreatmentPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/patient/treatment-plans")
@PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
public class PatientTreatmentPlanController {

    @Autowired
    private TreatmentPlanService planService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping
    public Result<List<TreatmentPlan>> getMyPlans(@RequestHeader("Authorization") String token) {
        Long patientId = jwtUtil.getUserIdFromToken(token.substring(7));
        return Result.success(planService.getPatientPlans(patientId));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> getPlanDetail(@PathVariable Long id) {
        Map<String, Object> detail = planService.getPlanDetail(id);
        if (detail == null) return Result.error("治疗计划不存在");
        return Result.success(detail);
    }
}
