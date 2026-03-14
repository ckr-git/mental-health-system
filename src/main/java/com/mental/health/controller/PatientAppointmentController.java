package com.mental.health.controller;

import com.mental.health.common.JwtUtil;
import com.mental.health.common.Result;
import com.mental.health.dto.BookAppointmentRequest;
import com.mental.health.dto.JoinWaitlistRequest;
import com.mental.health.dto.TimeSlotDTO;
import com.mental.health.entity.Appointment;
import com.mental.health.entity.AppointmentWaitlist;
import com.mental.health.service.SchedulingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/patient/appointments")
@PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
public class PatientAppointmentController {

    @Autowired
    private SchedulingService schedulingService;

    @Autowired
    private JwtUtil jwtUtil;

    private Long extractUserId(String token) {
        return jwtUtil.getUserIdFromToken(token.substring(7));
    }

    @GetMapping("/slots")
    public Result<List<TimeSlotDTO>> getAvailableSlots(
            @RequestParam Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<TimeSlotDTO> slots = schedulingService.getAvailableSlots(doctorId, startDate, endDate);
        return Result.success(slots);
    }

    @PostMapping("/book")
    public Result<Long> bookAppointment(@RequestBody BookAppointmentRequest request,
                                        @RequestHeader("Authorization") String token) {
        Long userId = extractUserId(token);
        try {
            Appointment apt = schedulingService.bookAppointment(userId, request);
            return Result.success("预约成功", apt.getId());
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping
    public Result<List<Appointment>> getMyAppointments(
            @RequestParam(required = false) Integer status,
            @RequestHeader("Authorization") String token) {
        Long userId = extractUserId(token);
        List<Appointment> list = schedulingService.getPatientAppointments(userId, status);
        return Result.success(list);
    }

    @PostMapping("/{id}/cancel")
    public Result<String> cancelAppointment(@PathVariable Long id,
                                            @RequestBody(required = false) Map<String, String> body,
                                            @RequestHeader("Authorization") String token) {
        Long userId = extractUserId(token);
        String reason = body != null ? body.get("reason") : null;
        try {
            schedulingService.patientCancelAppointment(userId, id, reason);
            return Result.success("取消成功");
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/waitlist")
    public Result<Long> joinWaitlist(@RequestBody JoinWaitlistRequest request,
                                     @RequestHeader("Authorization") String token) {
        Long userId = extractUserId(token);
        AppointmentWaitlist wl = new AppointmentWaitlist();
        wl.setDoctorId(request.getDoctorId());
        wl.setPreferredDate(request.getPreferredDate());
        wl.setPreferredTimeStart(request.getPreferredTimeStart());
        wl.setPreferredTimeEnd(request.getPreferredTimeEnd());
        wl.setAppointmentType(request.getAppointmentType());
        wl.setSymptoms(request.getSymptoms());
        AppointmentWaitlist saved = schedulingService.joinWaitlist(userId, wl);
        return Result.success("已加入等候名单", saved.getId());
    }

    @GetMapping("/waitlist")
    public Result<List<AppointmentWaitlist>> getMyWaitlist(@RequestHeader("Authorization") String token) {
        Long userId = extractUserId(token);
        return Result.success(schedulingService.getPatientWaitlist(userId));
    }

    @PostMapping("/waitlist/{id}/cancel")
    public Result<String> cancelWaitlist(@PathVariable Long id,
                                         @RequestHeader("Authorization") String token) {
        Long userId = extractUserId(token);
        if (schedulingService.cancelWaitlist(userId, id)) {
            return Result.success("已取消等候");
        }
        return Result.error("取消失败");
    }
}
