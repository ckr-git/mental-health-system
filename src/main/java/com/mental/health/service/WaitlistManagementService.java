package com.mental.health.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mental.health.dto.CreateNotificationCommand;
import com.mental.health.entity.*;
import com.mental.health.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 候补转正与到诊管理服务
 *
 * 核心能力:
 * 1. 候补优先级评分: 风险等级、是否复诊、等待时长、治疗计划需求
 * 2. Vacancy事件驱动: 预约取消/排班变更时自动匹配候补
 * 3. Offer管理: 发出、接受、拒绝、过期
 * 4. 锁位机制: 防止同一时段被多人抢占
 * 5. 到诊管理: 签到、未到诊、迟到取消
 */
@Service
public class WaitlistManagementService {

    private static final Logger log = LoggerFactory.getLogger(WaitlistManagementService.class);

    private static final int OFFER_HOLD_MINUTES = 120; // Offer有效期2小时
    private static final int SLOT_HOLD_MINUTES = 15;   // 锁位15分钟

    @Autowired private WaitlistOfferMapper offerMapper;
    @Autowired private AppointmentSlotHoldMapper holdMapper;
    @Autowired private AppointmentRescheduleLogMapper rescheduleLogMapper;
    @Autowired private AppointmentWaitlistMapper waitlistMapper;
    @Autowired private AppointmentMapper appointmentMapper;
    @Autowired private UserNotificationService notificationService;
    @Autowired private OutboxService outboxService;

    // ===== Vacancy事件处理 =====

    /**
     * 预约取消时触发vacancy匹配
     */
    @Transactional
    public void onAppointmentCancelled(Long appointmentId, Long cancelledBy, String reason) {
        Appointment appointment = appointmentMapper.selectById(appointmentId);
        if (appointment == null) return;

        // 记录取消
        appointment.setStatus(3); // 已取消
        appointment.setCancelledBy(cancelledBy);
        appointment.setCancelReason(reason);
        appointmentMapper.updateById(appointment);

        // 查找匹配的候补
        matchWaitlistToVacancy(appointment.getDoctorId(),
                appointment.getAppointmentTime().toLocalDate(),
                appointment.getAppointmentTime().toLocalTime(),
                appointment.getAppointmentTime().toLocalTime().plusMinutes(50));

        log.info("预约#{} 已取消, 开始候补匹配", appointmentId);
    }

    /**
     * 匹配候补到空缺时段
     */
    @Transactional
    public void matchWaitlistToVacancy(Long doctorId, LocalDate slotDate,
                                        LocalTime slotStart, LocalTime slotEnd) {
        // 检查时段是否已被锁定
        AppointmentSlotHold existingHold = holdMapper.findActiveHold(doctorId, slotDate, slotStart);
        if (existingHold != null) return;

        // 查找符合条件的候补，按优先级排序
        LambdaQueryWrapper<AppointmentWaitlist> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppointmentWaitlist::getDoctorId, doctorId)
                .eq(AppointmentWaitlist::getStatus, "WAITING")
                .le(AppointmentWaitlist::getPreferredDate, slotDate)
                .orderByDesc(AppointmentWaitlist::getPriorityScore)
                .orderByAsc(AppointmentWaitlist::getCreateTime)
                .last("LIMIT 3"); // 最多通知3人

        List<AppointmentWaitlist> candidates = waitlistMapper.selectList(wrapper);
        if (candidates.isEmpty()) return;

        // 给最高优先级的候选人发Offer
        AppointmentWaitlist topCandidate = candidates.get(0);
        createOffer(topCandidate, slotDate, slotStart, slotEnd);
    }

    // ===== Offer管理 =====

    @Transactional
    public Long createOffer(AppointmentWaitlist waitlistEntry,
                             LocalDate slotDate, LocalTime slotStart, LocalTime slotEnd) {
        // 创建锁位
        AppointmentSlotHold hold = new AppointmentSlotHold();
        hold.setDoctorId(waitlistEntry.getDoctorId());
        hold.setPatientId(waitlistEntry.getPatientId());
        hold.setSlotDate(slotDate);
        hold.setSlotStart(slotStart);
        hold.setSlotEnd(slotEnd);
        hold.setHoldStatus("HELD");
        hold.setHoldSource("WAITLIST_OFFER");
        hold.setSourceId(waitlistEntry.getId());
        hold.setHeldAt(LocalDateTime.now());
        hold.setExpireAt(LocalDateTime.now().plusMinutes(OFFER_HOLD_MINUTES));
        holdMapper.insert(hold);

        // 创建Offer
        WaitlistOffer offer = new WaitlistOffer();
        offer.setWaitlistId(waitlistEntry.getId());
        offer.setPatientId(waitlistEntry.getPatientId());
        offer.setDoctorId(waitlistEntry.getDoctorId());
        offer.setOfferedSlotDate(slotDate);
        offer.setOfferedSlotStart(slotStart);
        offer.setOfferedSlotEnd(slotEnd);
        offer.setOfferStatus("PENDING");
        offer.setPriorityScore(waitlistEntry.getPriorityScore());
        offer.setOfferedAt(LocalDateTime.now());
        offer.setExpireAt(LocalDateTime.now().plusMinutes(OFFER_HOLD_MINUTES));
        offerMapper.insert(offer);

        // 更新候补状态
        waitlistEntry.setStatus("NOTIFIED");
        waitlistEntry.setNotifiedAt(LocalDateTime.now());
        waitlistMapper.updateById(waitlistEntry);

        // 发送通知
        CreateNotificationCommand cmd = new CreateNotificationCommand();
        cmd.setUserId(waitlistEntry.getPatientId());
        cmd.setCategory("APPOINTMENT");
        cmd.setPriority("HIGH");
        cmd.setTitle("候补通知：有空余时段");
        cmd.setContent("您关注的医生在" + slotDate + " " + slotStart + "有空余时段，" +
                "请在" + OFFER_HOLD_MINUTES + "分钟内确认预约。");
        cmd.setActionType("ROUTE");
        cmd.setSourceType("WAITLIST_OFFER");
        cmd.setSourceId(offer.getId());
        try { notificationService.createNotification(cmd); } catch (Exception e) {
            log.error("发送候补通知失败: {}", e.getMessage());
        }

        log.info("候补Offer#{} 已发出, 患者#{}, 时段={} {}", offer.getId(),
                waitlistEntry.getPatientId(), slotDate, slotStart);
        return offer.getId();
    }

    /**
     * 患者接受Offer → 创建预约
     */
    @Transactional
    public Long acceptOffer(Long offerId, Long patientId) {
        WaitlistOffer offer = offerMapper.selectById(offerId);
        if (offer == null || !offer.getPatientId().equals(patientId)) {
            throw new RuntimeException("Offer不存在或不属于当前用户");
        }
        if (!"PENDING".equals(offer.getOfferStatus())) {
            throw new RuntimeException("Offer已过期或已处理");
        }
        if (offer.getExpireAt().isBefore(LocalDateTime.now())) {
            offer.setOfferStatus("EXPIRED");
            offerMapper.updateById(offer);
            throw new RuntimeException("Offer已过期");
        }

        // 创建预约
        Appointment appointment = new Appointment();
        appointment.setPatientId(patientId);
        appointment.setDoctorId(offer.getDoctorId());
        appointment.setAppointmentTime(LocalDateTime.of(offer.getOfferedSlotDate(), offer.getOfferedSlotStart()));
        appointment.setType("CONSULTATION");
        appointment.setStatus(1); // 已确认
        appointment.setSource("WAITLIST");
        appointment.setWaitlistId(offer.getWaitlistId());
        appointment.setPriorityCode("FOLLOWUP");
        appointmentMapper.insert(appointment);

        // 更新Offer
        offer.setOfferStatus("ACCEPTED");
        offer.setRespondedAt(LocalDateTime.now());
        offer.setResultingAppointmentId(appointment.getId());
        offerMapper.updateById(offer);

        // 转正锁位
        convertHold(offer.getDoctorId(), offer.getOfferedSlotDate(),
                offer.getOfferedSlotStart(), appointment.getId());

        // 更新候补状态
        AppointmentWaitlist waitlist = waitlistMapper.selectById(offer.getWaitlistId());
        if (waitlist != null) {
            waitlist.setStatus("BOOKED");
            waitlist.setAcceptedAt(LocalDateTime.now());
            waitlistMapper.updateById(waitlist);
        }

        log.info("候补转正: Offer#{} → 预约#{}, 患者#{}", offerId, appointment.getId(), patientId);
        return appointment.getId();
    }

    /**
     * 患者拒绝Offer
     */
    @Transactional
    public void declineOffer(Long offerId, Long patientId, String reason) {
        WaitlistOffer offer = offerMapper.selectById(offerId);
        if (offer == null || !offer.getPatientId().equals(patientId)) {
            throw new RuntimeException("Offer不存在或不属于当前用户");
        }

        offer.setOfferStatus("DECLINED");
        offer.setRespondedAt(LocalDateTime.now());
        offer.setDeclineReason(reason);
        offerMapper.updateById(offer);

        // 释放锁位
        releaseHold(offer.getDoctorId(), offer.getOfferedSlotDate(), offer.getOfferedSlotStart());

        // 恢复候补状态
        AppointmentWaitlist waitlist = waitlistMapper.selectById(offer.getWaitlistId());
        if (waitlist != null) {
            waitlist.setStatus("WAITING");
            waitlist.setDeclineReason(reason);
            waitlistMapper.updateById(waitlist);
        }

        // 尝试给下一个候选人发Offer
        matchWaitlistToVacancy(offer.getDoctorId(), offer.getOfferedSlotDate(),
                offer.getOfferedSlotStart(), offer.getOfferedSlotEnd());
    }

    // ===== 到诊管理 =====

    @Transactional
    public void checkIn(Long appointmentId, Long patientId) {
        Appointment appointment = appointmentMapper.selectById(appointmentId);
        if (appointment == null || !appointment.getPatientId().equals(patientId)) {
            throw new RuntimeException("预约不存在");
        }
        appointment.setAttendanceStatus("CHECKED_IN");
        appointment.setCheckInTime(LocalDateTime.now());
        appointmentMapper.updateById(appointment);
    }

    @Transactional
    public void markNoShow(Long appointmentId, Long operatorId) {
        Appointment appointment = appointmentMapper.selectById(appointmentId);
        if (appointment == null) throw new RuntimeException("预约不存在");

        appointment.setAttendanceStatus("NO_SHOW");
        appointmentMapper.updateById(appointment);

        // 发布事件供分析模块消费
        try {
            Map<String, Object> payload = Map.of(
                    "appointmentId", appointmentId,
                    "patientId", appointment.getPatientId(),
                    "doctorId", appointment.getDoctorId()
            );
            outboxService.append("APPOINTMENT", appointmentId, "APPOINTMENT_NO_SHOW",
                    "NO_SHOW:" + appointmentId, JSON.toJSONString(payload));
        } catch (Exception e) {
            log.error("发布爽约事件失败: {}", e.getMessage());
        }

        log.warn("患者#{} 预约#{} 未到诊", appointment.getPatientId(), appointmentId);
    }

    // ===== 改约 =====

    @Transactional
    public Long rescheduleAppointment(Long appointmentId, LocalDateTime newTime,
                                       Long operatorId, String operatorRole, String reason) {
        Appointment old = appointmentMapper.selectById(appointmentId);
        if (old == null) throw new RuntimeException("预约不存在");

        // 记录改约日志
        AppointmentRescheduleLog rescheduleLog = new AppointmentRescheduleLog();
        rescheduleLog.setAppointmentId(appointmentId);
        rescheduleLog.setOriginalTime(old.getAppointmentTime());
        rescheduleLog.setNewTime(newTime);
        rescheduleLog.setRescheduleReason(reason);
        rescheduleLog.setRescheduledBy(operatorId);
        rescheduleLog.setRescheduledByRole(operatorRole);
        rescheduleLogMapper.insert(rescheduleLog);

        // 更新预约时间
        old.setAppointmentTime(newTime);
        old.setRescheduledFromId(appointmentId);
        appointmentMapper.updateById(old);

        return appointmentId;
    }

    // ===== 优先级计算 =====

    public int calculatePriority(Long patientId, Long doctorId, boolean isFollowUp,
                                  String riskLevel, Long treatmentPlanId) {
        int score = 0;

        // 风险等级权重
        if ("CRITICAL".equals(riskLevel)) score += 100;
        else if ("HIGH".equals(riskLevel)) score += 60;
        else if ("MEDIUM".equals(riskLevel)) score += 30;

        // 复诊权重
        if (isFollowUp) score += 40;

        // 有治疗计划权重
        if (treatmentPlanId != null) score += 20;

        // 等待时长权重（每天+2分）
        LambdaQueryWrapper<AppointmentWaitlist> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppointmentWaitlist::getPatientId, patientId)
                .eq(AppointmentWaitlist::getDoctorId, doctorId)
                .eq(AppointmentWaitlist::getStatus, "WAITING")
                .orderByAsc(AppointmentWaitlist::getCreateTime)
                .last("LIMIT 1");
        AppointmentWaitlist existing = waitlistMapper.selectOne(wrapper);
        if (existing != null) {
            long waitDays = java.time.Duration.between(
                    existing.getCreateTime(), LocalDateTime.now()).toDays();
            score += (int) Math.min(waitDays * 2, 30); // 最多30分
        }

        return score;
    }

    // ===== 过期清理调度器 =====

    @Scheduled(fixedDelay = 60_000) // 每分钟
    public void cleanupExpired() {
        // 清理过期Offer
        List<WaitlistOffer> expiredOffers = offerMapper.findExpiredOffers();
        for (WaitlistOffer offer : expiredOffers) {
            offer.setOfferStatus("EXPIRED");
            offerMapper.updateById(offer);

            // 恢复候补状态
            AppointmentWaitlist waitlist = waitlistMapper.selectById(offer.getWaitlistId());
            if (waitlist != null && "NOTIFIED".equals(waitlist.getStatus())) {
                waitlist.setStatus("WAITING");
                waitlistMapper.updateById(waitlist);
            }

            // 释放锁位
            releaseHold(offer.getDoctorId(), offer.getOfferedSlotDate(), offer.getOfferedSlotStart());

            // 尝试给下一个候选人
            matchWaitlistToVacancy(offer.getDoctorId(), offer.getOfferedSlotDate(),
                    offer.getOfferedSlotStart(), offer.getOfferedSlotEnd());
        }

        // 清理过期锁位
        List<AppointmentSlotHold> expiredHolds = holdMapper.findExpiredHolds();
        for (AppointmentSlotHold hold : expiredHolds) {
            hold.setHoldStatus("EXPIRED");
            holdMapper.updateById(hold);
        }
    }

    // ===== 查询 =====

    public List<WaitlistOffer> getPatientOffers(Long patientId, String status) {
        LambdaQueryWrapper<WaitlistOffer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WaitlistOffer::getPatientId, patientId);
        if (status != null) wrapper.eq(WaitlistOffer::getOfferStatus, status);
        wrapper.orderByDesc(WaitlistOffer::getOfferedAt);
        return offerMapper.selectList(wrapper);
    }

    // ===== 内部方法 =====

    private void convertHold(Long doctorId, LocalDate slotDate, LocalTime slotStart, Long appointmentId) {
        AppointmentSlotHold hold = holdMapper.findActiveHold(doctorId, slotDate, slotStart);
        if (hold != null) {
            hold.setHoldStatus("CONVERTED");
            hold.setConvertedAt(LocalDateTime.now());
            hold.setResultingAppointmentId(appointmentId);
            holdMapper.updateById(hold);
        }
    }

    private void releaseHold(Long doctorId, LocalDate slotDate, LocalTime slotStart) {
        AppointmentSlotHold hold = holdMapper.findActiveHold(doctorId, slotDate, slotStart);
        if (hold != null) {
            hold.setHoldStatus("RELEASED");
            holdMapper.updateById(hold);
        }
    }
}
