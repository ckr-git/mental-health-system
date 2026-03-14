package com.mental.health.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mental.health.common.Result;
import com.mental.health.entity.Appointment;
import com.mental.health.service.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 预约管理控制器
 */
@RestController
@RequestMapping("/api/admin/appointments")
@PreAuthorize("hasRole('ADMIN')")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    /**
     * Get appointment statistics
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = appointmentService.getStatistics();
        return Result.success(stats);
    }

    /**
     * Get appointment list
     */
    @GetMapping
    public Result<IPage<Map<String, Object>>> getAppointments(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String appointmentType) {
        IPage<Map<String, Object>> appointments = appointmentService.getAppointments(
                pageNum, pageSize, doctorId, patientId, status, appointmentType
        );
        return Result.success(appointments);
    }

    /**
     * Get appointment detail
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getAppointmentDetail(@PathVariable Long id) {
        Map<String, Object> appointment = appointmentService.getAppointmentDetail(id);
        if (appointment != null) {
            return Result.success(appointment);
        } else {
            return Result.error("预约不存在");
        }
    }

    /**
     * Create appointment
     */
    @PostMapping
    public Result<String> createAppointment(@RequestBody Appointment appointment) {
        boolean success = appointmentService.createAppointment(appointment);
        if (success) {
            return Result.success("创建成功");
        } else {
            return Result.error("创建失败");
        }
    }

    /**
     * Update appointment
     */
    @PutMapping("/{id}")
    public Result<String> updateAppointment(@PathVariable Long id, @RequestBody Appointment appointment) {
        appointment.setId(id);
        boolean success = appointmentService.updateAppointment(appointment);
        if (success) {
            return Result.success("更新成功");
        } else {
            return Result.error("更新失败");
        }
    }

    /**
     * Confirm appointment
     */
    @PostMapping("/{id}/confirm")
    public Result<String> confirmAppointment(@PathVariable Long id) {
        boolean success = appointmentService.confirmAppointment(id);
        if (success) {
            return Result.success("确认成功");
        } else {
            return Result.error("确认失败");
        }
    }

    /**
     * Cancel appointment
     */
    @PostMapping("/{id}/cancel")
    public Result<String> cancelAppointment(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String reason = request.get("reason");
        boolean success = appointmentService.cancelAppointment(id, reason);
        if (success) {
            return Result.success("取消成功");
        } else {
            return Result.error("取消失败");
        }
    }

    /**
     * Complete appointment
     */
    @PostMapping("/{id}/complete")
    public Result<String> completeAppointment(@PathVariable Long id) {
        boolean success = appointmentService.completeAppointment(id);
        if (success) {
            return Result.success("完成成功");
        } else {
            return Result.error("完成失败");
        }
    }

    /**
     * Delete appointment
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteAppointment(@PathVariable Long id) {
        boolean success = appointmentService.deleteAppointment(id);
        if (success) {
            return Result.success("删除成功");
        } else {
            return Result.error("删除失败");
        }
    }

    /**
     * Get doctor statistics
     */
    @GetMapping("/doctor/{doctorId}/statistics")
    public Result<Map<String, Object>> getDoctorStatistics(@PathVariable Long doctorId) {
        Map<String, Object> stats = appointmentService.getDoctorStatistics(doctorId);
        return Result.success(stats);
    }

    /**
     * Get patient statistics
     */
    @GetMapping("/patient/{patientId}/statistics")
    public Result<Map<String, Object>> getPatientStatistics(@PathVariable Long patientId) {
        Map<String, Object> stats = appointmentService.getPatientStatistics(patientId);
        return Result.success(stats);
    }

    /**
     * Get trend data
     */
    @GetMapping("/trend")
    public Result<Map<String, Object>> getTrendData(@RequestParam(defaultValue = "7") int days) {
        Map<String, Object> trendData = appointmentService.getTrendData(days);
        return Result.success(trendData);
    }
}
