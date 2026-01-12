package com.mental.health.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mental.health.entity.Appointment;
import com.mental.health.entity.User;
import com.mental.health.mapper.AppointmentMapper;
import com.mental.health.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 预约服务
 */
@Service
public class AppointmentService {

    @Autowired
    private AppointmentMapper appointmentMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * Get paginated appointments
     */
    public IPage<Map<String, Object>> getAppointments(int pageNum, int pageSize, Long doctorId, Long patientId, Integer status, String appointmentType) {
        Page<Appointment> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();

        if (doctorId != null) {
            wrapper.eq(Appointment::getDoctorId, doctorId);
        }
        if (patientId != null) {
            wrapper.eq(Appointment::getPatientId, patientId);
        }
        if (status != null) {
            wrapper.eq(Appointment::getStatus, status);
        }
        if (StringUtils.hasText(appointmentType)) {
            wrapper.eq(Appointment::getAppointmentType, appointmentType);
        }

        wrapper.orderByDesc(Appointment::getAppointmentTime);
        IPage<Appointment> appointmentPage = appointmentMapper.selectPage(page, wrapper);

        // Enrich with user info
        IPage<Map<String, Object>> result = appointmentPage.convert(this::enrichAppointment);
        return result;
    }

    /**
     * Enrich appointment with user information
     */
    private Map<String, Object> enrichAppointment(Appointment appointment) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", appointment.getId());
        map.put("appointmentType", appointment.getAppointmentType());
        map.put("appointmentTime", appointment.getAppointmentTime());
        map.put("duration", appointment.getDuration());
        map.put("status", appointment.getStatus());
        map.put("symptoms", appointment.getSymptoms());
        map.put("notes", appointment.getNotes());
        map.put("cancelReason", appointment.getCancelReason());
        map.put("createTime", appointment.getCreateTime());
        map.put("confirmTime", appointment.getConfirmTime());
        map.put("completeTime", appointment.getCompleteTime());

        // Get patient info
        User patient = userMapper.selectById(appointment.getPatientId());
        if (patient != null) {
            map.put("patientId", patient.getId());
            map.put("patientName", patient.getNickname() != null ? patient.getNickname() : patient.getUsername());
            map.put("patientPhone", patient.getPhone());
        }

        // Get doctor info
        User doctor = userMapper.selectById(appointment.getDoctorId());
        if (doctor != null) {
            map.put("doctorId", doctor.getId());
            map.put("doctorName", doctor.getNickname() != null ? doctor.getNickname() : doctor.getUsername());
            map.put("doctorSpecialization", doctor.getSpecialization());
        }

        return map;
    }

    /**
     * Get appointment detail
     */
    public Map<String, Object> getAppointmentDetail(Long id) {
        Appointment appointment = appointmentMapper.selectById(id);
        if (appointment != null) {
            return enrichAppointment(appointment);
        }
        return null;
    }

    /**
     * Create appointment
     */
    @Transactional
    public boolean createAppointment(Appointment appointment) {
        if (appointment.getStatus() == null) {
            appointment.setStatus(0); // Default: pending confirmation
        }
        if (appointment.getReminderSent() == null) {
            appointment.setReminderSent(0);
        }
        return appointmentMapper.insert(appointment) > 0;
    }

    /**
     * Update appointment
     */
    @Transactional
    public boolean updateAppointment(Appointment appointment) {
        return appointmentMapper.updateById(appointment) > 0;
    }

    /**
     * Confirm appointment
     */
    @Transactional
    public boolean confirmAppointment(Long id) {
        Appointment appointment = appointmentMapper.selectById(id);
        if (appointment != null && appointment.getStatus() == 0) {
            appointment.setStatus(1);
            appointment.setConfirmTime(LocalDateTime.now());
            return appointmentMapper.updateById(appointment) > 0;
        }
        return false;
    }

    /**
     * Cancel appointment
     */
    @Transactional
    public boolean cancelAppointment(Long id, String reason) {
        Appointment appointment = appointmentMapper.selectById(id);
        if (appointment != null && (appointment.getStatus() == 0 || appointment.getStatus() == 1)) {
            appointment.setStatus(3);
            appointment.setCancelReason(reason);
            appointment.setCancelTime(LocalDateTime.now());
            return appointmentMapper.updateById(appointment) > 0;
        }
        return false;
    }

    /**
     * Complete appointment
     */
    @Transactional
    public boolean completeAppointment(Long id) {
        Appointment appointment = appointmentMapper.selectById(id);
        if (appointment != null && appointment.getStatus() == 1) {
            appointment.setStatus(2);
            appointment.setCompleteTime(LocalDateTime.now());
            return appointmentMapper.updateById(appointment) > 0;
        }
        return false;
    }

    /**
     * Delete appointment
     */
    @Transactional
    public boolean deleteAppointment(Long id) {
        return appointmentMapper.deleteById(id) > 0;
    }

    /**
     * Get appointment statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();

        // Total appointments
        stats.put("totalAppointments", appointmentMapper.selectCount(wrapper));

        // Pending confirmation
        wrapper.clear();
        wrapper.eq(Appointment::getStatus, 0);
        stats.put("pendingAppointments", appointmentMapper.selectCount(wrapper));

        // Confirmed
        wrapper.clear();
        wrapper.eq(Appointment::getStatus, 1);
        stats.put("confirmedAppointments", appointmentMapper.selectCount(wrapper));

        // Completed
        wrapper.clear();
        wrapper.eq(Appointment::getStatus, 2);
        stats.put("completedAppointments", appointmentMapper.selectCount(wrapper));

        // Cancelled
        wrapper.clear();
        wrapper.eq(Appointment::getStatus, 3);
        stats.put("cancelledAppointments", appointmentMapper.selectCount(wrapper));

        // Today's appointments
        wrapper.clear();
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        wrapper.between(Appointment::getAppointmentTime, startOfDay, endOfDay);
        stats.put("todayAppointments", appointmentMapper.selectCount(wrapper));

        return stats;
    }

    /**
     * Get doctor statistics
     */
    public Map<String, Object> getDoctorStatistics(Long doctorId) {
        Map<String, Object> stats = new HashMap<>();

        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Appointment::getDoctorId, doctorId);

        stats.put("totalAppointments", appointmentMapper.selectCount(wrapper));

        wrapper.clear();
        wrapper.eq(Appointment::getDoctorId, doctorId).eq(Appointment::getStatus, 0);
        stats.put("pendingAppointments", appointmentMapper.selectCount(wrapper));

        wrapper.clear();
        wrapper.eq(Appointment::getDoctorId, doctorId).eq(Appointment::getStatus, 2);
        stats.put("completedAppointments", appointmentMapper.selectCount(wrapper));

        // Get patient count
        wrapper.clear();
        wrapper.eq(Appointment::getDoctorId, doctorId);
        List<Appointment> appointments = appointmentMapper.selectList(wrapper);
        long uniquePatients = appointments.stream()
                .map(Appointment::getPatientId)
                .distinct()
                .count();
        stats.put("patientCount", uniquePatients);

        return stats;
    }

    /**
     * Get patient statistics
     */
    public Map<String, Object> getPatientStatistics(Long patientId) {
        Map<String, Object> stats = new HashMap<>();

        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Appointment::getPatientId, patientId);

        stats.put("totalAppointments", appointmentMapper.selectCount(wrapper));

        wrapper.clear();
        wrapper.eq(Appointment::getPatientId, patientId).eq(Appointment::getStatus, 1);
        stats.put("upcomingAppointments", appointmentMapper.selectCount(wrapper));

        wrapper.clear();
        wrapper.eq(Appointment::getPatientId, patientId).eq(Appointment::getStatus, 2);
        stats.put("completedAppointments", appointmentMapper.selectCount(wrapper));

        return stats;
    }

    /**
     * Get appointment trend data
     */
    public Map<String, Object> getTrendData(int days) {
        Map<String, Object> trendData = new HashMap<>();

        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(days - 1).withHour(0).withMinute(0).withSecond(0);

        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(Appointment::getCreateTime, startDate, endDate);
        List<Appointment> appointments = appointmentMapper.selectList(wrapper);

        // Group by date and count
        Map<String, Long> dailyCounts = appointments.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getCreateTime().toLocalDate().toString(),
                        Collectors.counting()
                ));

        trendData.put("dailyCounts", dailyCounts);
        trendData.put("totalInPeriod", appointments.size());

        return trendData;
    }
}
