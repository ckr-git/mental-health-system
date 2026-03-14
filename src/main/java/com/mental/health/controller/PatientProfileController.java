package com.mental.health.controller;

import com.mental.health.common.JwtUtil;
import com.mental.health.common.Result;
import com.mental.health.dto.ProfileAggregate;
import com.mental.health.dto.UpdateProfileCommand;
import com.mental.health.service.PatientDoctorRelationshipService;
import com.mental.health.service.PatientProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patient/profile")
public class PatientProfileController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PatientProfileService patientProfileService;

    @Autowired
    private PatientDoctorRelationshipService patientDoctorRelationshipService;

    @GetMapping
    public Result<ProfileAggregate> getProfile(
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        ProfileAggregate profile = patientProfileService.getAggregateProfile(userId);
        if (profile == null) {
            return Result.error("用户不存在");
        }
        return Result.success(profile);
    }

    @PutMapping
    public Result<ProfileAggregate> updateProfile(
            @RequestHeader("Authorization") String token,
            @RequestBody UpdateProfileCommand cmd) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        ProfileAggregate updated = patientProfileService.updateAggregateProfile(userId, cmd);
        return Result.success("档案更新成功", updated);
    }

    @GetMapping("/doctor/{patientId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public Result<ProfileAggregate> getPatientProfile(
            @RequestHeader("Authorization") String token,
            @PathVariable Long patientId) {
        String jwt = token.substring(7);
        Long requesterId = jwtUtil.getUserIdFromToken(jwt);
        String requesterRole = jwtUtil.getRoleFromToken(jwt);
        if ("DOCTOR".equals(requesterRole)
                && !patientDoctorRelationshipService.hasRelationship(requesterId, patientId)) {
            return Result.error(403, "无权查看该患者档案");
        }

        ProfileAggregate profile = patientProfileService.getAggregateProfile(patientId);
        if (profile == null) {
            return Result.error("患者不存在");
        }
        return Result.success(profile);
    }
}
