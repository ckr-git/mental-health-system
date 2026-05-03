package com.mental.health.controller;

import com.mental.health.common.JwtUtil;
import com.mental.health.common.Result;
import com.mental.health.entity.WaitlistOffer;
import com.mental.health.service.WaitlistManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class WaitlistManagementController {

    @Autowired private WaitlistManagementService waitlistService;
    @Autowired private JwtUtil jwtUtil;

    // ===== 患者端 =====

    @GetMapping("/patient/waitlist-offers")
    public Result<List<WaitlistOffer>> getMyOffers(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String status) {
        Long patientId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        return Result.success(waitlistService.getPatientOffers(patientId, status));
    }

    @PostMapping("/patient/waitlist-offers/{offerId}/accept")
    public Result<Long> acceptOffer(
            @RequestHeader("Authorization") String token,
            @PathVariable Long offerId) {
        Long patientId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        Long appointmentId = waitlistService.acceptOffer(offerId, patientId);
        return Result.success(appointmentId);
    }

    @PostMapping("/patient/waitlist-offers/{offerId}/decline")
    public Result<Void> declineOffer(
            @RequestHeader("Authorization") String token,
            @PathVariable Long offerId,
            @RequestBody Map<String, String> body) {
        Long patientId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        waitlistService.declineOffer(offerId, patientId, body.get("reason"));
        return Result.success();
    }

    @PostMapping("/patient/appointments/{appointmentId}/check-in")
    public Result<Void> checkIn(
            @RequestHeader("Authorization") String token,
            @PathVariable Long appointmentId) {
        Long patientId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        waitlistService.checkIn(appointmentId, patientId);
        return Result.success();
    }

    // ===== 医生端 =====

    @PostMapping("/doctor/appointments/{appointmentId}/no-show")
    public Result<Void> markNoShow(
            @RequestHeader("Authorization") String token,
            @PathVariable Long appointmentId) {
        Long doctorId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        waitlistService.markNoShow(appointmentId, doctorId);
        return Result.success();
    }

    @PostMapping("/doctor/appointments/{appointmentId}/reschedule")
    public Result<Long> reschedule(
            @RequestHeader("Authorization") String token,
            @PathVariable Long appointmentId,
            @RequestBody Map<String, String> body) {
        Long doctorId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        LocalDateTime newTime = LocalDateTime.parse(body.get("newTime"));
        Long id = waitlistService.rescheduleAppointment(
                appointmentId, newTime, doctorId, "DOCTOR", body.get("reason"));
        return Result.success(id);
    }

    @PostMapping("/doctor/appointments/{appointmentId}/cancel")
    public Result<Void> cancelAppointment(
            @RequestHeader("Authorization") String token,
            @PathVariable Long appointmentId,
            @RequestBody Map<String, String> body) {
        Long doctorId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        waitlistService.onAppointmentCancelled(appointmentId, doctorId, body.get("reason"));
        return Result.success();
    }
}
