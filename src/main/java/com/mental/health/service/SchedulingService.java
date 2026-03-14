package com.mental.health.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mental.health.dto.BookAppointmentRequest;
import com.mental.health.dto.TimeSlotDTO;
import com.mental.health.entity.*;
import com.mental.health.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SchedulingService {

    private static final Logger log = LoggerFactory.getLogger(SchedulingService.class);

    @Autowired
    private DoctorScheduleMapper scheduleMapper;

    @Autowired
    private DoctorScheduleOverrideMapper overrideMapper;

    @Autowired
    private AppointmentMapper appointmentMapper;

    @Autowired
    private AppointmentWaitlistMapper waitlistMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OutboxService outboxService;

    @Autowired
    private UserNotificationService notificationService;

    public List<DoctorSchedule> getDoctorSchedule(Long doctorId) {
        return scheduleMapper.findActiveByDoctorId(doctorId);
    }

    @Transactional
    public DoctorSchedule saveScheduleSlot(DoctorSchedule slot) {
        if (slot.getActive() == null) {
            slot.setActive(1);
        }
        if (slot.getMaxPatients() == null) {
            slot.setMaxPatients(1);
        }
        if (slot.getSlotDuration() == null) {
            slot.setSlotDuration(50);
        }

        // Check for overlapping schedule on same day
        LambdaQueryWrapper<DoctorSchedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DoctorSchedule::getDoctorId, slot.getDoctorId())
               .eq(DoctorSchedule::getDayOfWeek, slot.getDayOfWeek())
               .eq(DoctorSchedule::getActive, 1);
        if (slot.getId() != null) {
            wrapper.ne(DoctorSchedule::getId, slot.getId());
        }
        List<DoctorSchedule> existing = scheduleMapper.selectList(wrapper);
        for (DoctorSchedule s : existing) {
            if (timesOverlap(slot.getStartTime(), slot.getEndTime(), s.getStartTime(), s.getEndTime())) {
                throw new IllegalArgumentException("排班时段与已有时段冲突：" +
                        s.getStartTime() + "-" + s.getEndTime());
            }
        }

        if (slot.getId() != null) {
            scheduleMapper.updateById(slot);
        } else {
            scheduleMapper.insert(slot);
        }
        return slot;
    }

    @Transactional
    public void deleteScheduleSlot(Long doctorId, Long slotId) {
        DoctorSchedule slot = scheduleMapper.selectById(slotId);
        if (slot == null || !slot.getDoctorId().equals(doctorId)) {
            throw new IllegalArgumentException("排班不存在");
        }
        scheduleMapper.deleteById(slotId);
    }

    @Transactional
    public DoctorScheduleOverride saveOverride(Long doctorId, DoctorScheduleOverride override) {
        override.setDoctorId(doctorId);
        DoctorScheduleOverride existing = overrideMapper.findByDoctorAndDate(doctorId, override.getOverrideDate());
        if (existing != null) {
            override.setId(existing.getId());
            overrideMapper.updateById(override);
        } else {
            overrideMapper.insert(override);
        }
        return override;
    }

    public List<DoctorScheduleOverride> getOverrides(Long doctorId, LocalDate startDate, LocalDate endDate) {
        return overrideMapper.findByDoctorAndDateRange(doctorId, startDate, endDate);
    }

    /**
     * Get available time slots for a doctor on a specific date range.
     */
    public List<TimeSlotDTO> getAvailableSlots(Long doctorId, LocalDate startDate, LocalDate endDate) {
        List<DoctorSchedule> weeklySchedule = scheduleMapper.findActiveByDoctorId(doctorId);
        List<DoctorScheduleOverride> overrides = overrideMapper.findByDoctorAndDateRange(doctorId, startDate, endDate);

        List<TimeSlotDTO> slots = new ArrayList<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            List<TimeSlotDTO> daySlots = buildDaySlots(doctorId, current, weeklySchedule, overrides);
            slots.addAll(daySlots);
            current = current.plusDays(1);
        }
        return slots;
    }

    private List<TimeSlotDTO> buildDaySlots(Long doctorId, LocalDate date,
                                             List<DoctorSchedule> weeklySchedule,
                                             List<DoctorScheduleOverride> overrides) {
        List<TimeSlotDTO> result = new ArrayList<>();

        // Check override first
        DoctorScheduleOverride override = overrides.stream()
                .filter(o -> o.getOverrideDate().equals(date))
                .findFirst().orElse(null);

        if (override != null) {
            if ("UNAVAILABLE".equals(override.getOverrideType())) {
                return result; // Day off
            }
            // Use override times
            int slotMin = override.getSlotDuration() != null ? override.getSlotDuration() : 50;
            generateSlots(result, doctorId, date, override.getStartTime(), override.getEndTime(), slotMin, 1, null);
            return result;
        }

        // Use weekly schedule
        int javaDow = date.getDayOfWeek().getValue(); // 1=Monday...7=Sunday
        for (DoctorSchedule sched : weeklySchedule) {
            if (sched.getDayOfWeek().equals(javaDow)) {
                generateSlots(result, doctorId, date, sched.getStartTime(), sched.getEndTime(),
                        sched.getSlotDuration(), sched.getMaxPatients(), sched.getLocation());
            }
        }
        return result;
    }

    private void generateSlots(List<TimeSlotDTO> result, Long doctorId, LocalDate date,
                                LocalTime start, LocalTime end, int slotMinutes,
                                int maxPatients, String location) {
        LocalTime slotStart = start;
        while (slotStart.plusMinutes(slotMinutes).compareTo(end) <= 0) {
            LocalTime slotEnd = slotStart.plusMinutes(slotMinutes);

            // Skip past slots
            if (date.equals(LocalDate.now()) && slotStart.isBefore(LocalTime.now())) {
                slotStart = slotEnd;
                continue;
            }

            // Count existing bookings for this slot
            int booked = countBookingsForSlot(doctorId, date, slotStart, slotEnd);
            int available = maxPatients - booked;

            if (available > 0) {
                result.add(new TimeSlotDTO(date, slotStart, slotEnd, available, maxPatients, location));
            }
            slotStart = slotEnd;
        }
    }

    private int countBookingsForSlot(Long doctorId, LocalDate date, LocalTime slotStart, LocalTime slotEnd) {
        LocalDateTime start = LocalDateTime.of(date, slotStart);
        LocalDateTime end = LocalDateTime.of(date, slotEnd);
        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Appointment::getDoctorId, doctorId)
               .ge(Appointment::getAppointmentTime, start)
               .lt(Appointment::getAppointmentTime, end)
               .in(Appointment::getStatus, 0, 1); // pending or confirmed
        return Math.toIntExact(appointmentMapper.selectCount(wrapper));
    }

    /**
     * Patient books an appointment with conflict detection.
     */
    @Transactional
    public Appointment bookAppointment(Long patientId, BookAppointmentRequest request) {
        // 1. Validate doctor exists
        User doctor = userMapper.selectById(request.getDoctorId());
        if (doctor == null || !"DOCTOR".equals(doctor.getRole())) {
            throw new IllegalArgumentException("医生不存在");
        }

        LocalDateTime aptTime = request.getAppointmentTime();
        LocalDate aptDate = aptTime.toLocalDate();
        LocalTime aptTimeOfDay = aptTime.toLocalTime();

        // 2. Check the slot is within doctor's schedule
        int slotDuration = findSlotDuration(request.getDoctorId(), aptDate, aptTimeOfDay);
        if (slotDuration <= 0) {
            throw new IllegalArgumentException("所选时间不在医生的出诊时段内");
        }

        // 3. Check slot availability
        int booked = countBookingsForSlot(request.getDoctorId(), aptDate, aptTimeOfDay,
                aptTimeOfDay.plusMinutes(slotDuration));
        int maxPatients = findMaxPatients(request.getDoctorId(), aptDate, aptTimeOfDay);
        if (booked >= maxPatients) {
            throw new IllegalArgumentException("该时段已满，请选择其他时段或加入等候名单");
        }

        // 4. Check patient doesn't have conflicting appointment
        LambdaQueryWrapper<Appointment> conflictWrapper = new LambdaQueryWrapper<>();
        conflictWrapper.eq(Appointment::getPatientId, patientId)
                       .eq(Appointment::getAppointmentTime, aptTime)
                       .in(Appointment::getStatus, 0, 1);
        if (appointmentMapper.selectCount(conflictWrapper) > 0) {
            throw new IllegalArgumentException("您在该时间段已有预约");
        }

        // 5. Create appointment
        Appointment appointment = new Appointment();
        appointment.setPatientId(patientId);
        appointment.setDoctorId(request.getDoctorId());
        appointment.setAppointmentTime(aptTime);
        appointment.setDuration(slotDuration);
        appointment.setAppointmentType(request.getAppointmentType() != null ?
                request.getAppointmentType() : "CONSULTATION");
        appointment.setSymptoms(request.getSymptoms());
        appointment.setNotes(request.getNotes());
        appointment.setStatus(0); // pending
        appointment.setReminderSent(0);
        appointmentMapper.insert(appointment);

        // 6. Notify doctor via outbox
        String eventKey = "APPOINTMENT_BOOKED:" + appointment.getId();
        String payload = String.format("{\"appointmentId\":%d,\"patientId\":%d,\"doctorId\":%d,\"time\":\"%s\"}",
                appointment.getId(), patientId, request.getDoctorId(), aptTime);
        outboxService.append("APPOINTMENT", appointment.getId(),
                "APPOINTMENT_BOOKED", eventKey, payload);

        log.info("Patient {} booked appointment {} with doctor {} at {}",
                patientId, appointment.getId(), request.getDoctorId(), aptTime);

        return appointment;
    }

    private int findSlotDuration(Long doctorId, LocalDate date, LocalTime time) {
        DoctorScheduleOverride override = overrideMapper.findByDoctorAndDate(doctorId, date);
        if (override != null) {
            if ("UNAVAILABLE".equals(override.getOverrideType())) return 0;
            if (override.getStartTime() != null && override.getEndTime() != null) {
                if (!time.isBefore(override.getStartTime()) && time.isBefore(override.getEndTime())) {
                    return override.getSlotDuration() != null ? override.getSlotDuration() : 50;
                }
            }
            return 0;
        }

        int dow = date.getDayOfWeek().getValue();
        List<DoctorSchedule> schedules = scheduleMapper.findActiveByDoctorId(doctorId);
        for (DoctorSchedule s : schedules) {
            if (s.getDayOfWeek().equals(dow) &&
                !time.isBefore(s.getStartTime()) && time.isBefore(s.getEndTime())) {
                return s.getSlotDuration();
            }
        }
        return 0;
    }

    private int findMaxPatients(Long doctorId, LocalDate date, LocalTime time) {
        int dow = date.getDayOfWeek().getValue();
        List<DoctorSchedule> schedules = scheduleMapper.findActiveByDoctorId(doctorId);
        for (DoctorSchedule s : schedules) {
            if (s.getDayOfWeek().equals(dow) &&
                !time.isBefore(s.getStartTime()) && time.isBefore(s.getEndTime())) {
                return s.getMaxPatients();
            }
        }
        return 1;
    }

    /**
     * Patient cancels their own appointment.
     */
    @Transactional
    public boolean patientCancelAppointment(Long patientId, Long appointmentId, String reason) {
        Appointment apt = appointmentMapper.selectById(appointmentId);
        if (apt == null || !apt.getPatientId().equals(patientId)) {
            throw new IllegalArgumentException("预约不存在");
        }
        if (apt.getStatus() != 0 && apt.getStatus() != 1) {
            throw new IllegalArgumentException("当前状态不可取消");
        }

        apt.setStatus(3);
        apt.setCancelReason(reason);
        apt.setCancelTime(LocalDateTime.now());
        appointmentMapper.updateById(apt);

        // Publish cancel event for waitlist processing
        String eventKey = "APPOINTMENT_CANCELLED:" + apt.getId();
        String payload = String.format("{\"appointmentId\":%d,\"doctorId\":%d,\"time\":\"%s\"}",
                apt.getId(), apt.getDoctorId(), apt.getAppointmentTime());
        outboxService.append("APPOINTMENT", apt.getId(),
                "APPOINTMENT_CANCELLED", eventKey, payload);

        return true;
    }

    /**
     * Doctor confirms an appointment.
     */
    @Transactional
    public boolean doctorConfirmAppointment(Long doctorId, Long appointmentId) {
        Appointment apt = appointmentMapper.selectById(appointmentId);
        if (apt == null || !apt.getDoctorId().equals(doctorId)) {
            throw new IllegalArgumentException("预约不存在");
        }
        if (apt.getStatus() != 0) {
            throw new IllegalArgumentException("当前状态不可确认");
        }

        apt.setStatus(1);
        apt.setConfirmTime(LocalDateTime.now());
        appointmentMapper.updateById(apt);

        String eventKey = "APPOINTMENT_CONFIRMED:" + apt.getId();
        String payload = String.format("{\"appointmentId\":%d,\"patientId\":%d,\"doctorId\":%d,\"time\":\"%s\"}",
                apt.getId(), apt.getPatientId(), doctorId, apt.getAppointmentTime());
        outboxService.append("APPOINTMENT", apt.getId(),
                "APPOINTMENT_CONFIRMED", eventKey, payload);

        return true;
    }

    /**
     * Doctor completes an appointment.
     */
    @Transactional
    public boolean doctorCompleteAppointment(Long doctorId, Long appointmentId) {
        Appointment apt = appointmentMapper.selectById(appointmentId);
        if (apt == null || !apt.getDoctorId().equals(doctorId)) {
            throw new IllegalArgumentException("预约不存在");
        }
        if (apt.getStatus() != 1) {
            throw new IllegalArgumentException("只有已确认的预约才能完成");
        }

        apt.setStatus(2);
        apt.setCompleteTime(LocalDateTime.now());
        appointmentMapper.updateById(apt);

        String eventKey = "APPOINTMENT_COMPLETED:" + apt.getId();
        String payload = String.format("{\"appointmentId\":%d,\"patientId\":%d,\"doctorId\":%d}",
                apt.getId(), apt.getPatientId(), doctorId);
        outboxService.append("APPOINTMENT", apt.getId(),
                "APPOINTMENT_COMPLETED", eventKey, payload);

        return true;
    }

    /**
     * Doctor cancels an appointment.
     */
    @Transactional
    public boolean doctorCancelAppointment(Long doctorId, Long appointmentId, String reason) {
        Appointment apt = appointmentMapper.selectById(appointmentId);
        if (apt == null || !apt.getDoctorId().equals(doctorId)) {
            throw new IllegalArgumentException("预约不存在");
        }
        if (apt.getStatus() != 0 && apt.getStatus() != 1) {
            throw new IllegalArgumentException("当前状态不可取消");
        }

        apt.setStatus(3);
        apt.setCancelReason(reason);
        apt.setCancelTime(LocalDateTime.now());
        appointmentMapper.updateById(apt);

        String eventKey = "DOCTOR_CANCELLED_APPOINTMENT:" + apt.getId();
        String payload = String.format("{\"appointmentId\":%d,\"patientId\":%d,\"doctorId\":%d,\"time\":\"%s\",\"reason\":\"%s\"}",
                apt.getId(), apt.getPatientId(), doctorId, apt.getAppointmentTime(),
                reason != null ? reason.replace("\"", "\\\"") : "");
        outboxService.append("APPOINTMENT", apt.getId(),
                "APPOINTMENT_CANCELLED", eventKey, payload);

        return true;
    }

    /**
     * Patient joins the waitlist.
     */
    @Transactional
    public AppointmentWaitlist joinWaitlist(Long patientId, AppointmentWaitlist waitlist) {
        waitlist.setPatientId(patientId);
        waitlist.setStatus("WAITING");
        waitlistMapper.insert(waitlist);
        return waitlist;
    }

    public List<AppointmentWaitlist> getPatientWaitlist(Long patientId) {
        return waitlistMapper.findActiveByPatient(patientId);
    }

    @Transactional
    public boolean cancelWaitlist(Long patientId, Long waitlistId) {
        AppointmentWaitlist wl = waitlistMapper.selectById(waitlistId);
        if (wl == null || !wl.getPatientId().equals(patientId)) {
            return false;
        }
        wl.setStatus("CANCELLED");
        waitlistMapper.updateById(wl);
        return true;
    }

    /**
     * Get patient's own appointments.
     */
    public List<Appointment> getPatientAppointments(Long patientId, Integer status) {
        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Appointment::getPatientId, patientId);
        if (status != null) {
            wrapper.eq(Appointment::getStatus, status);
        }
        wrapper.orderByDesc(Appointment::getAppointmentTime);
        return appointmentMapper.selectList(wrapper);
    }

    /**
     * Get doctor's own appointments.
     */
    public List<Appointment> getDoctorAppointments(Long doctorId, Integer status, LocalDate date) {
        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Appointment::getDoctorId, doctorId);
        if (status != null) {
            wrapper.eq(Appointment::getStatus, status);
        }
        if (date != null) {
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(23, 59, 59);
            wrapper.between(Appointment::getAppointmentTime, dayStart, dayEnd);
        }
        wrapper.orderByAsc(Appointment::getAppointmentTime);
        return appointmentMapper.selectList(wrapper);
    }

    private boolean timesOverlap(LocalTime s1, LocalTime e1, LocalTime s2, LocalTime e2) {
        return s1.isBefore(e2) && s2.isBefore(e1);
    }
}
