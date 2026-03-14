package com.mental.health.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mental.health.dto.CreateNotificationCommand;
import com.mental.health.entity.Appointment;
import com.mental.health.entity.User;
import com.mental.health.mapper.AppointmentMapper;
import com.mental.health.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class AppointmentReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(AppointmentReminderScheduler.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MM月dd日 HH:mm");

    @Autowired
    private AppointmentMapper appointmentMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserNotificationService notificationService;

    /**
     * Every 5 minutes, find confirmed appointments starting within 1 hour
     * that haven't been reminded yet, and send reminders.
     */
    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void sendReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourLater = now.plusHours(1);

        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Appointment::getStatus, 1)  // confirmed
               .eq(Appointment::getReminderSent, 0)
               .between(Appointment::getAppointmentTime, now, oneHourLater);

        List<Appointment> upcoming = appointmentMapper.selectList(wrapper);
        if (upcoming.isEmpty()) return;

        log.info("Found {} appointments to remind", upcoming.size());

        for (Appointment apt : upcoming) {
            try {
                sendReminderNotification(apt);
                apt.setReminderSent(1);
                appointmentMapper.updateById(apt);
            } catch (Exception e) {
                log.error("Failed to send reminder for appointment {}", apt.getId(), e);
            }
        }
    }

    /**
     * Every 10 minutes, expire pending appointments that are past their time.
     */
    @Scheduled(fixedDelay = 600_000)
    @Transactional
    public void expirePastAppointments() {
        LocalDateTime now = LocalDateTime.now();

        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Appointment::getStatus, 0)  // pending
               .lt(Appointment::getAppointmentTime, now);

        List<Appointment> expired = appointmentMapper.selectList(wrapper);
        for (Appointment apt : expired) {
            apt.setStatus(4); // expired
            appointmentMapper.updateById(apt);
            log.info("Expired appointment {}", apt.getId());
        }
    }

    private void sendReminderNotification(Appointment apt) {
        String timeStr = apt.getAppointmentTime().format(FMT);

        // Remind patient
        User doctor = userMapper.selectById(apt.getDoctorId());
        String doctorName = doctor != null ?
                (doctor.getNickname() != null ? doctor.getNickname() : doctor.getUsername()) : "医生";

        CreateNotificationCommand patientNotif = new CreateNotificationCommand();
        patientNotif.setUserId(apt.getPatientId());
        patientNotif.setCategory("APPOINTMENT");
        patientNotif.setPriority("HIGH");
        patientNotif.setTitle("预约提醒");
        patientNotif.setContent("您与" + doctorName + "的预约将于" + timeStr + "开始，请提前做好准备。");
        patientNotif.setSourceType("APPOINTMENT");
        patientNotif.setSourceId(apt.getId());
        notificationService.createNotification(patientNotif);

        // Remind doctor
        User patient = userMapper.selectById(apt.getPatientId());
        String patientName = patient != null ?
                (patient.getNickname() != null ? patient.getNickname() : patient.getUsername()) : "患者";

        CreateNotificationCommand doctorNotif = new CreateNotificationCommand();
        doctorNotif.setUserId(apt.getDoctorId());
        doctorNotif.setCategory("APPOINTMENT");
        doctorNotif.setPriority("NORMAL");
        doctorNotif.setTitle("预约提醒");
        doctorNotif.setContent("患者" + patientName + "的预约将于" + timeStr + "开始。");
        doctorNotif.setSourceType("APPOINTMENT");
        doctorNotif.setSourceId(apt.getId());
        notificationService.createNotification(doctorNotif);
    }
}
