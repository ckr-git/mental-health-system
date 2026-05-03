package com.mental.health.controller;

import com.mental.health.common.JwtUtil;
import com.mental.health.common.Result;
import com.mental.health.entity.*;
import com.mental.health.service.MeditationPrescriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class MeditationPrescriptionController {

    @Autowired private MeditationPrescriptionService prescriptionService;
    @Autowired private JwtUtil jwtUtil;

    // ===== 医生端 =====
    @PostMapping("/doctor/patients/{patientId}/meditation-prescriptions")
    public Result<Long> createPrescription(@RequestHeader("Authorization") String token,
            @PathVariable Long patientId, @RequestBody Map<String, Object> body) {
        Long doctorId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        Long exerciseId = Long.valueOf(body.get("exerciseId").toString());
        Long planId = body.get("treatmentPlanId") != null ? Long.valueOf(body.get("treatmentPlanId").toString()) : null;
        Integer sessionsPerWeek = body.get("sessionsPerWeek") != null ? Integer.valueOf(body.get("sessionsPerWeek").toString()) : null;
        Integer minutesPerSession = body.get("minutesPerSession") != null ? Integer.valueOf(body.get("minutesPerSession").toString()) : null;
        Long id = prescriptionService.createPrescription(patientId, doctorId, exerciseId,
                (String) body.get("goalCode"), (String) body.get("frequencyCode"),
                sessionsPerWeek, minutesPerSession, planId);
        return Result.success(id);
    }

    @PostMapping("/doctor/meditation-prescriptions/{id}/activate")
    public Result<Void> activate(@PathVariable Long id) {
        prescriptionService.activatePrescription(id);
        return Result.success();
    }

    @PostMapping("/doctor/meditation-prescriptions/{id}/pause")
    public Result<Void> pause(@PathVariable Long id) {
        prescriptionService.pausePrescription(id, null);
        return Result.success();
    }

    @PostMapping("/doctor/meditation-prescriptions/{id}/complete")
    public Result<Void> complete(@PathVariable Long id) {
        prescriptionService.completePrescription(id);
        return Result.success();
    }

    @GetMapping("/doctor/patients/{patientId}/meditation-prescriptions")
    public Result<List<MeditationPrescription>> getDoctorView(@PathVariable Long patientId,
            @RequestParam(required = false) String status) {
        return Result.success(prescriptionService.getPatientPrescriptions(patientId, status));
    }

    @GetMapping("/doctor/patients/{patientId}/meditation-effects/summary")
    public Result<Map<String, Object>> getEffectSummary(@PathVariable Long patientId,
            @RequestParam(defaultValue = "30") int days) {
        return Result.success(prescriptionService.summarizeEffect(patientId, days));
    }

    // ===== 患者端 =====
    @GetMapping("/patient/meditation/prescriptions")
    public Result<List<MeditationPrescription>> getMyPrescriptions(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String status) {
        Long patientId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        return Result.success(prescriptionService.getPatientPrescriptions(patientId, status));
    }

    @PostMapping("/patient/meditation/sessions/{sessionId}/effect")
    public Result<Long> recordEffect(@RequestHeader("Authorization") String token,
            @PathVariable Long sessionId, @RequestBody Map<String, Object> body) {
        Long patientId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        Long prescriptionId = body.get("prescriptionId") != null ? Long.valueOf(body.get("prescriptionId").toString()) : null;
        Integer preMood = body.get("preMoodScore") != null ? Integer.valueOf(body.get("preMoodScore").toString()) : null;
        Integer postMood = body.get("postMoodScore") != null ? Integer.valueOf(body.get("postMoodScore").toString()) : null;
        Integer preStress = body.get("preStressScore") != null ? Integer.valueOf(body.get("preStressScore").toString()) : null;
        Integer postStress = body.get("postStressScore") != null ? Integer.valueOf(body.get("postStressScore").toString()) : null;
        Integer benefit = body.get("perceivedBenefitScore") != null ? Integer.valueOf(body.get("perceivedBenefitScore").toString()) : null;
        return Result.success(prescriptionService.recordEffect(sessionId, prescriptionId, patientId,
                preMood, postMood, preStress, postStress, benefit, (String) body.get("note")));
    }

    @GetMapping("/patient/meditation/effects")
    public Result<List<MeditationEffectLog>> getMyEffects(@RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "20") int limit) {
        Long patientId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        return Result.success(prescriptionService.getPatientEffectLogs(patientId, limit));
    }
}
