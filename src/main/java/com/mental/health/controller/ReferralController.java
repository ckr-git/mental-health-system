package com.mental.health.controller;

import com.mental.health.common.JwtUtil;
import com.mental.health.common.Result;
import com.mental.health.entity.*;
import com.mental.health.service.ReferralService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/doctor")
public class ReferralController {

    @Autowired private ReferralService referralService;
    @Autowired private JwtUtil jwtUtil;

    // ===== 转诊 =====
    @PostMapping("/referrals")
    public Result<Long> initiateReferral(@RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> body) {
        Long fromDoctorId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        Long patientId = Long.valueOf(body.get("patientId").toString());
        Long toDoctorId = body.get("toDoctorId") != null ? Long.valueOf(body.get("toDoctorId").toString()) : null;
        Long planId = body.get("treatmentPlanId") != null ? Long.valueOf(body.get("treatmentPlanId").toString()) : null;
        return Result.success(referralService.initiateReferral(patientId, fromDoctorId, toDoctorId,
                (String) body.get("referralType"), (String) body.get("urgencyLevel"),
                (String) body.get("reason"), (String) body.get("clinicalSummary"), planId));
    }

    @GetMapping("/referrals")
    public Result<List<ReferralCase>> getReferrals(@RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "from") String role, @RequestParam(required = false) String status) {
        Long doctorId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        return Result.success(referralService.getDoctorReferrals(doctorId, role, status));
    }

    @PostMapping("/referrals/{id}/accept")
    public Result<Void> accept(@RequestHeader("Authorization") String token, @PathVariable Long id) {
        Long doctorId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        referralService.acceptReferral(id, doctorId);
        return Result.success();
    }

    @PostMapping("/referrals/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long doctorId = 0L; // simplified
        referralService.rejectReferral(id, doctorId, body.get("reason"));
        return Result.success();
    }

    @PostMapping("/referrals/{id}/handoffs")
    public Result<Long> createHandoff(@RequestHeader("Authorization") String token,
            @PathVariable Long id, @RequestBody ReferralHandoff handoff) {
        Long doctorId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        return Result.success(referralService.createHandoff(id, doctorId, handoff));
    }

    @PostMapping("/referral-handoffs/{id}/acknowledge")
    public Result<Void> ackHandoff(@RequestHeader("Authorization") String token, @PathVariable Long id) {
        Long doctorId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        referralService.acknowledgeHandoff(id, doctorId);
        return Result.success();
    }

    @PostMapping("/referrals/{id}/complete")
    public Result<Void> complete(@RequestHeader("Authorization") String token, @PathVariable Long id) {
        Long doctorId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        referralService.completeReferral(id, doctorId);
        return Result.success();
    }

    // ===== 护理团队 =====
    @GetMapping("/patients/{patientId}/care-team")
    public Result<List<CareTeamMember>> getCareTeam(@PathVariable Long patientId) {
        return Result.success(referralService.getActiveTeamMembers(patientId));
    }

    @PostMapping("/patients/{patientId}/care-team")
    public Result<Long> addMember(@RequestHeader("Authorization") String token,
            @PathVariable Long patientId, @RequestBody Map<String, Object> body) {
        Long addedBy = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        Long doctorId = Long.valueOf(body.get("doctorId").toString());
        return Result.success(referralService.addCareTeamMember(patientId, doctorId,
                (String) body.get("memberRoleCode"), addedBy, (String) body.get("accessScopeJson")));
    }

    @DeleteMapping("/care-team-members/{memberId}")
    public Result<Void> removeMember(@PathVariable Long memberId, @RequestParam(required = false) String reason) {
        referralService.removeCareTeamMember(memberId, reason);
        return Result.success();
    }
}
