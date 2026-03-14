package com.mental.health.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mental.health.dto.CreateNotificationCommand;
import com.mental.health.entity.OutboxEvent;
import com.mental.health.entity.User;
import com.mental.health.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class AppointmentEventHandler implements OutboxEventHandler {

    private static final Logger log = LoggerFactory.getLogger(AppointmentEventHandler.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MM月dd日 HH:mm");

    @Autowired
    private UserNotificationService notificationService;

    @Autowired
    private UserMapper userMapper;

    @Override
    public boolean supports(String eventType) {
        return "APPOINTMENT_BOOKED".equals(eventType) ||
               "APPOINTMENT_CONFIRMED".equals(eventType) ||
               "APPOINTMENT_CANCELLED".equals(eventType) ||
               "APPOINTMENT_COMPLETED".equals(eventType);
    }

    @Override
    public void handle(OutboxEvent event) {
        JSONObject payload = JSON.parseObject(event.getPayload());
        Long appointmentId = payload.getLong("appointmentId");
        Long patientId = payload.getLong("patientId");
        Long doctorId = payload.getLong("doctorId");
        String timeStr = payload.getString("time");

        switch (event.getEventType()) {
            case "APPOINTMENT_BOOKED":
                handleBooked(doctorId, patientId, timeStr, appointmentId);
                break;
            case "APPOINTMENT_CONFIRMED":
                handleConfirmed(patientId, doctorId, timeStr, appointmentId);
                break;
            case "APPOINTMENT_CANCELLED":
                handleCancelled(payload, appointmentId);
                break;
            case "APPOINTMENT_COMPLETED":
                handleCompleted(patientId, appointmentId);
                break;
            default:
                log.warn("Unknown appointment event: {}", event.getEventType());
        }
    }

    private void handleBooked(Long doctorId, Long patientId, String timeStr, Long appointmentId) {
        User patient = userMapper.selectById(patientId);
        String patientName = patient != null ?
                (patient.getNickname() != null ? patient.getNickname() : patient.getUsername()) : "患者";

        String formattedTime = formatTime(timeStr);

        CreateNotificationCommand cmd = new CreateNotificationCommand();
        cmd.setUserId(doctorId);
        cmd.setCategory("APPOINTMENT");
        cmd.setPriority("NORMAL");
        cmd.setTitle("新预约");
        cmd.setContent("患者" + patientName + "预约了" + formattedTime + "的咨询，请及时确认。");
        cmd.setSourceType("APPOINTMENT");
        cmd.setSourceId(appointmentId);
        notificationService.createNotification(cmd);
    }

    private void handleConfirmed(Long patientId, Long doctorId, String timeStr, Long appointmentId) {
        User doctor = userMapper.selectById(doctorId);
        String doctorName = doctor != null ?
                (doctor.getNickname() != null ? doctor.getNickname() : doctor.getUsername()) : "医生";

        String formattedTime = formatTime(timeStr);

        CreateNotificationCommand cmd = new CreateNotificationCommand();
        cmd.setUserId(patientId);
        cmd.setCategory("APPOINTMENT");
        cmd.setPriority("NORMAL");
        cmd.setTitle("预约已确认");
        cmd.setContent(doctorName + "已确认您" + formattedTime + "的预约。");
        cmd.setSourceType("APPOINTMENT");
        cmd.setSourceId(appointmentId);
        notificationService.createNotification(cmd);
    }

    private void handleCancelled(JSONObject payload, Long appointmentId) {
        Long patientId = payload.getLong("patientId");
        Long doctorId = payload.getLong("doctorId");
        String reason = payload.getString("reason");

        // Notify the other party
        if (patientId != null) {
            CreateNotificationCommand cmd = new CreateNotificationCommand();
            cmd.setUserId(patientId);
            cmd.setCategory("APPOINTMENT");
            cmd.setPriority("NORMAL");
            cmd.setTitle("预约已取消");
            cmd.setContent("您的预约已被取消" + (reason != null ? "，原因：" + reason : "。"));
            cmd.setSourceType("APPOINTMENT");
            cmd.setSourceId(appointmentId);
            notificationService.createNotification(cmd);
        }
    }

    private void handleCompleted(Long patientId, Long appointmentId) {
        CreateNotificationCommand cmd = new CreateNotificationCommand();
        cmd.setUserId(patientId);
        cmd.setCategory("APPOINTMENT");
        cmd.setPriority("LOW");
        cmd.setTitle("咨询已完成");
        cmd.setContent("您的咨询已完成，如有需要请在线沟通或预约下次咨询。");
        cmd.setSourceType("APPOINTMENT");
        cmd.setSourceId(appointmentId);
        notificationService.createNotification(cmd);
    }

    private String formatTime(String isoTime) {
        if (isoTime == null) return "";
        try {
            LocalDateTime dt = LocalDateTime.parse(isoTime);
            return dt.format(FMT);
        } catch (Exception e) {
            return isoTime;
        }
    }
}
