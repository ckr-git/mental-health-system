package com.mental.health.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mental.health.entity.Appointment;
import com.mental.health.entity.AssessmentReport;
import com.mental.health.entity.User;
import com.mental.health.mapper.AppointmentMapper;
import com.mental.health.mapper.AssessmentReportMapper;
import com.mental.health.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 医生服务
 */
@Service
public class DoctorService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AppointmentMapper appointmentMapper;

    @Autowired
    private AssessmentReportMapper assessmentReportMapper;

    @Autowired
    private PatientDoctorRelationshipService patientDoctorRelationshipService;

    /**
     * Get doctor's dashboard statistics
     */
    public Map<String, Object> getDashboardStatistics(String username) {
        User doctor = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));

        if (doctor == null) {
            return new HashMap<>();
        }

        Long doctorId = doctor.getId();
        Map<String, Object> stats = new HashMap<>();

        // Total patients count (from patient_doctor_relationship table)
        Long totalPatients = patientDoctorRelationshipService.getDoctorPatientCount(doctorId);
        stats.put("totalPatients", totalPatients);

        // Get appointments for other stats
        LambdaQueryWrapper<Appointment> appointmentWrapper = new LambdaQueryWrapper<>();
        appointmentWrapper.eq(Appointment::getDoctorId, doctorId);
        List<Appointment> allAppointments = appointmentMapper.selectList(appointmentWrapper);

        // Total appointments
        stats.put("totalAppointments", allAppointments.size());

        // Pending appointments (status = 0)
        long pendingAppointments = allAppointments.stream()
                .filter(a -> a.getStatus() == 0)
                .count();
        stats.put("pendingAppointments", pendingAppointments);

        // Today's appointments
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        long todayAppointments = allAppointments.stream()
                .filter(a -> a.getAppointmentTime().isAfter(startOfDay) &&
                            a.getAppointmentTime().isBefore(endOfDay))
                .count();
        stats.put("todayAppointments", todayAppointments);

        // Total reports
        LambdaQueryWrapper<AssessmentReport> reportWrapper = new LambdaQueryWrapper<>();
        reportWrapper.eq(AssessmentReport::getDoctorId, doctorId);
        long totalReports = assessmentReportMapper.selectCount(reportWrapper);
        stats.put("totalReports", totalReports);

        // Recent reports (last 7 days)
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        reportWrapper.ge(AssessmentReport::getCreateTime, weekAgo);
        long recentReports = assessmentReportMapper.selectCount(reportWrapper);
        stats.put("recentReports", recentReports);

        return stats;
    }

    /**
     * Get doctor's patients with pagination
     */
    public Map<String, Object> getPatients(String username, int pageNum, int pageSize, String keyword) {
        User doctor = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));

        if (doctor == null) {
            return new HashMap<>();
        }

        Long doctorId = doctor.getId();

        // Get patients from relationship table
        List<Map<String, Object>> allPatients = patientDoctorRelationshipService.getDoctorPatients(doctorId);

        // Filter by keyword if provided
        if (StringUtils.hasText(keyword)) {
            allPatients = allPatients.stream()
                    .filter(p -> {
                        String name = (String) p.get("patientName");
                        String uname = (String) p.get("patientUsername");
                        return (name != null && name.contains(keyword)) ||
                               (uname != null && uname.contains(keyword));
                    })
                    .collect(Collectors.toList());
        }

        // Manual pagination
        int total = allPatients.size();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);

        List<Map<String, Object>> pagedList;
        if (fromIndex < total) {
            pagedList = allPatients.subList(fromIndex, toIndex);
        } else {
            pagedList = new ArrayList<>();
        }

        // Transform to expected format
        List<Map<String, Object>> result = pagedList.stream()
                .map(p -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", p.get("patientId"));
                    map.put("username", p.get("patientUsername"));
                    map.put("nickname", p.get("patientName"));
                    map.put("phone", p.get("patientPhone"));
                    map.put("email", p.get("patientEmail"));
                    map.put("gender", p.get("patientGender"));
                    map.put("age", p.get("patientAge"));
                    map.put("createTime", p.get("createTime"));
                    map.put("appointmentCount", 0);
                    return map;
                })
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("records", result);
        response.put("total", total);
        response.put("pageNum", pageNum);
        response.put("pageSize", pageSize);

        return response;
    }

    /**
     * Get patient detail
     */
    public Map<String, Object> getPatientDetail(String username, Long patientId) {
        User doctor = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));

        if (doctor == null) {
            return null;
        }

        Long doctorId = doctor.getId();

        // Verify doctor has access to this patient (through patient_doctor_relationship)
        if (!patientDoctorRelationshipService.hasRelationship(doctorId, patientId)) {
            return null; // No permission
        }

        User patient = userMapper.selectById(patientId);
        if (patient == null || !"PATIENT".equals(patient.getRole())) {
            return null;
        }

        Map<String, Object> detail = new HashMap<>();
        detail.put("id", patient.getId());
        detail.put("username", patient.getUsername());
        detail.put("nickname", patient.getNickname());
        detail.put("email", patient.getEmail());
        detail.put("phone", patient.getPhone());
        detail.put("gender", patient.getGender());
        detail.put("age", patient.getAge());
        detail.put("status", patient.getStatus());
        detail.put("createTime", patient.getCreateTime());

        // Get appointment statistics
        LambdaQueryWrapper<Appointment> appointmentWrapper = new LambdaQueryWrapper<>();
        appointmentWrapper.eq(Appointment::getDoctorId, doctorId)
                .eq(Appointment::getPatientId, patientId);
        List<Appointment> appointments = appointmentMapper.selectList(appointmentWrapper);
        detail.put("totalAppointments", appointments.size());
        detail.put("completedAppointments", appointments.stream().filter(a -> a.getStatus() == 2).count());
        detail.put("cancelledAppointments", appointments.stream().filter(a -> a.getStatus() == 3).count());

        // Query patients with assessment reports count
        LambdaQueryWrapper<AssessmentReport> reportWrapper = new LambdaQueryWrapper<>();
        reportWrapper.eq(AssessmentReport::getUserId, patientId)
                .eq(AssessmentReport::getDoctorId, doctorId);
        long reportCount = assessmentReportMapper.selectCount(reportWrapper);
        detail.put("assessmentReports", reportCount);

        return detail;
    }

    /**
     * Get doctor's assessment reports
     */
    public Map<String, Object> getReports(String username, int pageNum, int pageSize, Long patientId) {
        User doctor = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));

        if (doctor == null) {
            return new HashMap<>();
        }

        Long doctorId = doctor.getId();

        Page<AssessmentReport> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AssessmentReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssessmentReport::getDoctorId, doctorId);
        if (patientId != null) {
            wrapper.eq(AssessmentReport::getUserId, patientId);
        }
        wrapper.orderByDesc(AssessmentReport::getCreateTime);

        IPage<AssessmentReport> reportPage = assessmentReportMapper.selectPage(page, wrapper);

        // Enrich with patient info
        List<Map<String, Object>> enrichedReports = reportPage.getRecords().stream()
                .map(report -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", report.getId());
                    map.put("patientId", report.getUserId());
                    map.put("reportType", report.getReportType());
                    map.put("assessmentDate", report.getCreateTime());
                    map.put("summary", report.getSummary());
                    map.put("recommendations", report.getSuggestions());
                    map.put("createTime", report.getCreateTime());

                    // Add patient name
                    User patient = userMapper.selectById(report.getUserId());
                    if (patient != null) {
                        map.put("patientName", patient.getNickname() != null ?
                                patient.getNickname() : patient.getUsername());
                    }

                    return map;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("records", enrichedReports);
        result.put("total", reportPage.getTotal());
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);

        return result;
    }

    /**
     * Get report detail
     */
    public AssessmentReport getReportDetail(String username, Long reportId) {
        User doctor = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));

        if (doctor == null) {
            return null;
        }

        AssessmentReport report = assessmentReportMapper.selectById(reportId);
        if (report == null || !report.getDoctorId().equals(doctor.getId())) {
            return null; // No permission
        }

        return report;
    }

    /**
     * Create assessment report
     */
    @Transactional
    public boolean createReport(String username, AssessmentReport report) {
        User doctor = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));

        if (doctor == null) {
            return false;
        }

        report.setDoctorId(doctor.getId());
        // Note: userId should be set from the controller's patientId parameter
        return assessmentReportMapper.insert(report) > 0;
    }

    /**
     * Update assessment report
     */
    @Transactional
    public boolean updateReport(String username, AssessmentReport report) {
        User doctor = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));

        if (doctor == null) {
            return false;
        }

        AssessmentReport existing = assessmentReportMapper.selectById(report.getId());
        if (existing == null || !existing.getDoctorId().equals(doctor.getId())) {
            return false; // No permission
        }

        return assessmentReportMapper.updateById(report) > 0;
    }

    /**
     * Delete assessment report
     */
    @Transactional
    public boolean deleteReport(String username, Long reportId) {
        User doctor = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));

        if (doctor == null) {
            return false;
        }

        AssessmentReport report = assessmentReportMapper.selectById(reportId);
        if (report == null || !report.getDoctorId().equals(doctor.getId())) {
            return false; // No permission
        }

        return assessmentReportMapper.deleteById(reportId) > 0;
    }

    /**
     * Get doctor's appointments
     */
    public Map<String, Object> getAppointments(String username, int pageNum, int pageSize, Integer status) {
        User doctor = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));

        if (doctor == null) {
            return new HashMap<>();
        }

        Long doctorId = doctor.getId();

        Page<Appointment> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Appointment::getDoctorId, doctorId);
        if (status != null) {
            wrapper.eq(Appointment::getStatus, status);
        }
        wrapper.orderByDesc(Appointment::getAppointmentTime);

        IPage<Appointment> appointmentPage = appointmentMapper.selectPage(page, wrapper);

        // Enrich with patient info
        List<Map<String, Object>> enrichedAppointments = appointmentPage.getRecords().stream()
                .map(appointment -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", appointment.getId());
                    map.put("patientId", appointment.getPatientId());
                    map.put("appointmentType", appointment.getAppointmentType());
                    map.put("appointmentTime", appointment.getAppointmentTime());
                    map.put("duration", appointment.getDuration());
                    map.put("status", appointment.getStatus());
                    map.put("symptoms", appointment.getSymptoms());
                    map.put("notes", appointment.getNotes());

                    // Add patient name
                    User patient = userMapper.selectById(appointment.getPatientId());
                    if (patient != null) {
                        map.put("patientName", patient.getNickname() != null ?
                                patient.getNickname() : patient.getUsername());
                    }

                    return map;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("records", enrichedAppointments);
        result.put("total", appointmentPage.getTotal());
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);

        return result;
    }

    /**
     * Get recent appointments for dashboard
     */
    public List<Map<String, Object>> getRecentAppointments(String username) {
        User doctor = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));

        if (doctor == null) {
            return List.of();
        }

        Long doctorId = doctor.getId();

        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Appointment::getDoctorId, doctorId);
        wrapper.orderByDesc(Appointment::getAppointmentTime);
        wrapper.last("LIMIT 10");

        List<Appointment> appointments = appointmentMapper.selectList(wrapper);

        return appointments.stream()
                .map(appointment -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", appointment.getId());
                    map.put("patientId", appointment.getPatientId());
                    map.put("appointmentType", appointment.getAppointmentType());
                    map.put("appointmentTime", appointment.getAppointmentTime());
                    map.put("status", appointment.getStatus());

                    User patient = userMapper.selectById(appointment.getPatientId());
                    if (patient != null) {
                        map.put("patientName", patient.getNickname() != null ?
                                patient.getNickname() : patient.getUsername());
                    }

                    return map;
                })
                .collect(Collectors.toList());
    }
}
