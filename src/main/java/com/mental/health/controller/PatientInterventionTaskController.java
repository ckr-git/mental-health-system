package com.mental.health.controller;

import com.mental.health.common.JwtUtil;
import com.mental.health.common.Result;
import com.mental.health.entity.InterventionTask;
import com.mental.health.service.TreatmentPlanPhaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/patient")
public class PatientInterventionTaskController {

    @Autowired private TreatmentPlanPhaseService phaseService;
    @Autowired private JwtUtil jwtUtil;

    @GetMapping("/intervention-tasks")
    public Result<List<InterventionTask>> getMyTasks(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String status) {
        Long patientId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        return Result.success(phaseService.getPatientTasks(patientId, status));
    }

    @PostMapping("/intervention-tasks/{taskId}/submit")
    public Result<Void> submitTask(
            @RequestHeader("Authorization") String token,
            @PathVariable Long taskId,
            @RequestBody Map<String, String> body) {
        Long patientId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        phaseService.submitTask(taskId, patientId, body.get("note"), body.get("value"));
        return Result.success();
    }
}
