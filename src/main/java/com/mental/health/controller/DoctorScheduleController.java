package com.mental.health.controller;

import com.mental.health.common.JwtUtil;
import com.mental.health.common.Result;
import com.mental.health.dto.ScheduleOverrideRequest;
import com.mental.health.dto.TimeSlotDTO;
import com.mental.health.entity.Appointment;
import com.mental.health.entity.DoctorSchedule;
import com.mental.health.entity.DoctorScheduleOverride;
import com.mental.health.service.SchedulingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/doctor/schedule")
@PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
public class DoctorScheduleController {

    @Autowired
    private SchedulingService schedulingService;

    @Autowired
    private JwtUtil jwtUtil;

    private Long extractUserId(String token) {
        return jwtUtil.getUserIdFromToken(token.substring(7));
    }

    @GetMapping
    public Result<List<DoctorSchedule>> getMySchedule(@RequestHeader("Authorization") String token) {
        Long doctorId = extractUserId(token);
        return Result.success(schedulingService.getDoctorSchedule(doctorId));
    }

    @PostMapping
    public Result<DoctorSchedule> saveScheduleSlot(@RequestBody DoctorSchedule slot,
                                                    @RequestHeader("Authorization") String token) {
        Long doctorId = extractUserId(token);
        slot.setDoctorId(doctorId);
        try {
            DoctorSchedule saved = schedulingService.saveScheduleSlot(slot);
            return Result.success("保存成功", saved);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<String> deleteScheduleSlot(@PathVariable Long id,
                                              @RequestHeader("Authorization") String token) {
        Long doctorId = extractUserId(token);
        try {
            schedulingService.deleteScheduleSlot(doctorId, id);
            return Result.success("删除成功");
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/override")
    public Result<DoctorScheduleOverride> saveOverride(@RequestBody ScheduleOverrideRequest request,
                                                        @RequestHeader("Authorization") String token) {
        Long doctorId = extractUserId(token);
        DoctorScheduleOverride override = new DoctorScheduleOverride();
        override.setOverrideDate(request.getOverrideDate());
        override.setOverrideType(request.getOverrideType());
        if (request.getStartTime() != null) {
            override.setStartTime(LocalTime.parse(request.getStartTime(), DateTimeFormatter.ofPattern("HH:mm")));
        }
        if (request.getEndTime() != null) {
            override.setEndTime(LocalTime.parse(request.getEndTime(), DateTimeFormatter.ofPattern("HH:mm")));
        }
        override.setSlotDuration(request.getSlotDuration());
        override.setReason(request.getReason());
        DoctorScheduleOverride saved = schedulingService.saveOverride(doctorId, override);
        return Result.success("保存成功", saved);
    }

    @GetMapping("/overrides")
    public Result<List<DoctorScheduleOverride>> getOverrides(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestHeader("Authorization") String token) {
        Long doctorId = extractUserId(token);
        return Result.success(schedulingService.getOverrides(doctorId, startDate, endDate));
    }

    @GetMapping("/slots")
    public Result<List<TimeSlotDTO>> getMySlots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestHeader("Authorization") String token) {
        Long doctorId = extractUserId(token);
        return Result.success(schedulingService.getAvailableSlots(doctorId, startDate, endDate));
    }

    @GetMapping("/appointments")
    public Result<List<Appointment>> getMyAppointments(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestHeader("Authorization") String token) {
        Long doctorId = extractUserId(token);
        return Result.success(schedulingService.getDoctorAppointments(doctorId, status, date));
    }

    @PostMapping("/appointments/{id}/confirm")
    public Result<String> confirmAppointment(@PathVariable Long id,
                                              @RequestHeader("Authorization") String token) {
        Long doctorId = extractUserId(token);
        try {
            schedulingService.doctorConfirmAppointment(doctorId, id);
            return Result.success("确认成功");
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/appointments/{id}/complete")
    public Result<String> completeAppointment(@PathVariable Long id,
                                               @RequestHeader("Authorization") String token) {
        Long doctorId = extractUserId(token);
        try {
            schedulingService.doctorCompleteAppointment(doctorId, id);
            return Result.success("完成成功");
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/appointments/{id}/cancel")
    public Result<String> cancelAppointment(@PathVariable Long id,
                                            @RequestBody(required = false) Map<String, String> body,
                                            @RequestHeader("Authorization") String token) {
        Long doctorId = extractUserId(token);
        String reason = body != null ? body.get("reason") : null;
        try {
            schedulingService.doctorCancelAppointment(doctorId, id, reason);
            return Result.success("取消成功");
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }
}
