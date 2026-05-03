package com.mental.health.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mental.health.entity.*;
import com.mental.health.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * 分析聚合服务 — 患者日指标/医生负载/分析作业
 *
 * 每夜批处理: aggregatePatientDailyMetric + aggregateDoctorWorkload
 * 支持手动触发和增量重算
 */
@Service
public class AnalyticsAggregationService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsAggregationService.class);

    @Autowired private PatientDailyMetricMapper metricMapper;
    @Autowired private DoctorWorkloadSnapshotMapper workloadMapper;
    @Autowired private AnalyticsJobLogMapper jobLogMapper;
    @Autowired private MoodDiaryMapper moodDiaryMapper;
    @Autowired private AppointmentMapper appointmentMapper;
    @Autowired private MeditationSessionMapper meditationSessionMapper;
    @Autowired private InterventionTaskMapper taskMapper;
    @Autowired private RiskEventMapper riskEventMapper;
    @Autowired private CrisisCaseMapper crisisCaseMapper;
    @Autowired private TreatmentPlanMapper planMapper;
    @Autowired private TreatmentPlanReviewMapper reviewMapper;
    @Autowired private ReferralCaseMapper referralCaseMapper;
    @Autowired private AiHandoffTaskMapper handoffTaskMapper;
    @Autowired private AppointmentWaitlistMapper waitlistMapper;
    @Autowired private UserMapper userMapper;

    // ===== 每夜批处理 =====

    @Scheduled(cron = "0 3 2 * * ?") // 每天凌晨2:03
    public void nightlyBatch() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("开始每夜分析批处理, 目标日期: {}", yesterday);

        Long jobId = startJob("NIGHTLY_BATCH", "PATIENT_DAILY", yesterday);
        try {
            int patientCount = aggregatePatientDailyMetrics(yesterday);
            int doctorCount = aggregateDoctorWorkloads(yesterday);
            finishJob(jobId, "SUCCESS", patientCount + doctorCount, null);
            log.info("每夜批处理完成: 患者{}条, 医生{}条", patientCount, doctorCount);
        } catch (Exception e) {
            finishJob(jobId, "FAILED", 0, e.getMessage());
            log.error("每夜批处理失败: {}", e.getMessage(), e);
        }
    }

    // ===== 患者日指标 =====

    @Transactional
    public int aggregatePatientDailyMetrics(LocalDate targetDate) {
        LocalDateTime dayStart = targetDate.atStartOfDay();
        LocalDateTime dayEnd = targetDate.atTime(LocalTime.MAX);

        // 查找当天有活动的患者
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(User::getRole, "PATIENT").eq(User::getStatus, 1);
        List<User> patients = userMapper.selectList(userWrapper);

        int count = 0;
        for (User patient : patients) {
            try {
                PatientDailyMetric metric = calculatePatientMetric(patient.getId(), dayStart, dayEnd);
                if (metric != null) {
                    // Upsert
                    LambdaQueryWrapper<PatientDailyMetric> existWrapper = new LambdaQueryWrapper<>();
                    existWrapper.eq(PatientDailyMetric::getPatientId, patient.getId())
                            .eq(PatientDailyMetric::getMetricAt, dayStart);
                    PatientDailyMetric existing = metricMapper.selectOne(existWrapper);

                    if (existing != null) {
                        metric.setId(existing.getId());
                        metric.setMetricStatus("RECALCULATED");
                        metricMapper.updateById(metric);
                    } else {
                        metricMapper.insert(metric);
                    }
                    count++;
                }
            } catch (Exception e) {
                log.warn("计算患者#{} 日指标失败: {}", patient.getId(), e.getMessage());
            }
        }
        return count;
    }

    private PatientDailyMetric calculatePatientMetric(Long patientId, LocalDateTime start, LocalDateTime end) {
        // 查询日记
        LambdaQueryWrapper<MoodDiary> diaryW = new LambdaQueryWrapper<>();
        diaryW.eq(MoodDiary::getUserId, patientId)
                .between(MoodDiary::getCreateTime, start, end);
        List<MoodDiary> diaries = moodDiaryMapper.selectList(diaryW);

        // 查询风险事件
        LambdaQueryWrapper<RiskEvent> riskW = new LambdaQueryWrapper<>();
        riskW.eq(RiskEvent::getUserId, patientId)
                .between(RiskEvent::getDetectedAt, start, end);
        long riskCount = riskEventMapper.selectCount(riskW);

        // 查询爽约
        LambdaQueryWrapper<Appointment> apptW = new LambdaQueryWrapper<>();
        apptW.eq(Appointment::getPatientId, patientId)
                .between(Appointment::getAppointmentTime, start, end)
                .eq(Appointment::getAttendanceStatus, "NO_SHOW");
        long noShowCount = appointmentMapper.selectCount(apptW);

        // 查询冥想
        LambdaQueryWrapper<MeditationSession> medW = new LambdaQueryWrapper<>();
        medW.eq(MeditationSession::getUserId, patientId)
                .eq(MeditationSession::getSessionStatus, "COMPLETED")
                .between(MeditationSession::getStartedAt, start, end);
        List<MeditationSession> medSessions = meditationSessionMapper.selectList(medW);
        int medMinutes = medSessions.stream()
                .mapToInt(s -> s.getActualSeconds() != null ? s.getActualSeconds() / 60 : 0)
                .sum();

        // 任务完成率
        LambdaQueryWrapper<InterventionTask> taskW = new LambdaQueryWrapper<>();
        taskW.eq(InterventionTask::getPatientId, patientId)
                .between(InterventionTask::getDueAt, start, end);
        List<InterventionTask> tasks = taskMapper.selectList(taskW);
        BigDecimal taskRate = null;
        if (!tasks.isEmpty()) {
            long completed = tasks.stream().filter(t -> "COMPLETED".equals(t.getTaskStatus())).count();
            taskRate = BigDecimal.valueOf(completed).divide(BigDecimal.valueOf(tasks.size()), 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        }

        // 如果没有任何活动数据，跳过
        if (diaries.isEmpty() && riskCount == 0 && noShowCount == 0 && medMinutes == 0 && tasks.isEmpty()) {
            return null;
        }

        PatientDailyMetric metric = new PatientDailyMetric();
        metric.setPatientId(patientId);
        metric.setMetricAt(start);
        metric.setMetricStatus("CALCULATED");
        metric.setDiaryCount(diaries.size());
        metric.setRiskEventCount((int) riskCount);
        metric.setNoShowCount((int) noShowCount);
        metric.setMeditationMinutes(medMinutes);
        metric.setTaskCompletionRate(taskRate);
        metric.setSourceWindowStart(start);
        metric.setSourceWindowEnd(end);

        if (!diaries.isEmpty()) {
            metric.setMoodAvg(avgOf(diaries, MoodDiary::getMoodScore));
            metric.setSleepAvg(avgOf(diaries, MoodDiary::getSleepQuality));
            metric.setStressAvg(avgOf(diaries, MoodDiary::getStressLevel));
            metric.setEnergyAvg(avgOf(diaries, MoodDiary::getEnergyLevel));
        }

        return metric;
    }

    // ===== 医生工作负载 =====

    @Transactional
    public int aggregateDoctorWorkloads(LocalDate targetDate) {
        LambdaQueryWrapper<User> doctorW = new LambdaQueryWrapper<>();
        doctorW.eq(User::getRole, "DOCTOR").eq(User::getStatus, 1);
        List<User> doctors = userMapper.selectList(doctorW);

        int count = 0;
        for (User doctor : doctors) {
            try {
                DoctorWorkloadSnapshot snapshot = calculateDoctorWorkload(doctor.getId(), targetDate);
                // Upsert
                LambdaQueryWrapper<DoctorWorkloadSnapshot> existW = new LambdaQueryWrapper<>();
                existW.eq(DoctorWorkloadSnapshot::getDoctorId, doctor.getId())
                        .eq(DoctorWorkloadSnapshot::getSnapshotAt, targetDate.atStartOfDay());
                DoctorWorkloadSnapshot existing = workloadMapper.selectOne(existW);

                if (existing != null) {
                    snapshot.setId(existing.getId());
                    snapshot.setSnapshotStatus("RECALCULATED");
                    workloadMapper.updateById(snapshot);
                } else {
                    workloadMapper.insert(snapshot);
                }
                count++;
            } catch (Exception e) {
                log.warn("计算医生#{} 工作负载失败: {}", doctor.getId(), e.getMessage());
            }
        }
        return count;
    }

    private DoctorWorkloadSnapshot calculateDoctorWorkload(Long doctorId, LocalDate targetDate) {
        LocalDateTime dayStart = targetDate.atStartOfDay();
        LocalDateTime dayEnd = targetDate.atTime(LocalTime.MAX);

        DoctorWorkloadSnapshot s = new DoctorWorkloadSnapshot();
        s.setDoctorId(doctorId);
        s.setSnapshotAt(dayStart);
        s.setSnapshotStatus("CALCULATED");

        // 预约统计
        LambdaQueryWrapper<Appointment> apptW = new LambdaQueryWrapper<>();
        apptW.eq(Appointment::getDoctorId, doctorId).between(Appointment::getAppointmentTime, dayStart, dayEnd);
        List<Appointment> appts = appointmentMapper.selectList(apptW);
        s.setScheduledAppointments(appts.size());
        s.setCompletedAppointments((int) appts.stream().filter(a -> a.getStatus() != null && a.getStatus() == 2).count());
        s.setNoShowCount((int) appts.stream().filter(a -> "NO_SHOW".equals(a.getAttendanceStatus())).count());

        // 危机案例
        LambdaQueryWrapper<CrisisCase> crisisW = new LambdaQueryWrapper<>();
        crisisW.eq(CrisisCase::getOwnerDoctorId, doctorId)
                .notIn(CrisisCase::getCaseStatus, "RESOLVED", "POST_REVIEW");
        s.setCrisisOpenCount(crisisCaseMapper.selectCount(crisisW).intValue());

        LambdaQueryWrapper<CrisisCase> overdueW = new LambdaQueryWrapper<>();
        overdueW.eq(CrisisCase::getOwnerDoctorId, doctorId)
                .notIn(CrisisCase::getCaseStatus, "RESOLVED", "POST_REVIEW")
                .le(CrisisCase::getSlaDeadline, LocalDateTime.now());
        s.setCrisisOverdueCount(crisisCaseMapper.selectCount(overdueW).intValue());

        // 治疗计划
        LambdaQueryWrapper<TreatmentPlan> planW = new LambdaQueryWrapper<>();
        planW.eq(TreatmentPlan::getDoctorId, doctorId).eq(TreatmentPlan::getPlanStatus, "ACTIVE");
        s.setActivePlans(planMapper.selectCount(planW).intValue());

        // 转诊
        LambdaQueryWrapper<ReferralCase> refW = new LambdaQueryWrapper<>();
        refW.eq(ReferralCase::getToDoctorId, doctorId)
                .in(ReferralCase::getReferralStatus, "INITIATED", "ACCEPTED", "HANDOFF_READY");
        s.setActiveReferrals(referralCaseMapper.selectCount(refW).intValue());

        // 评审
        LambdaQueryWrapper<TreatmentPlanReview> revW = new LambdaQueryWrapper<>();
        revW.eq(TreatmentPlanReview::getReviewerId, doctorId)
                .eq(TreatmentPlanReview::getReviewStatus, "PENDING");
        s.setPendingReviews(reviewMapper.selectCount(revW).intValue());

        // 转人工任务
        LambdaQueryWrapper<AiHandoffTask> handoffW = new LambdaQueryWrapper<>();
        handoffW.eq(AiHandoffTask::getAssignedDoctorId, doctorId)
                .in(AiHandoffTask::getTaskStatus, "OPEN", "ACKNOWLEDGED");
        s.setPendingHandoffTasks(handoffTaskMapper.selectCount(handoffW).intValue());

        // 候补
        LambdaQueryWrapper<AppointmentWaitlist> waitW = new LambdaQueryWrapper<>();
        waitW.eq(AppointmentWaitlist::getDoctorId, doctorId)
                .eq(AppointmentWaitlist::getStatus, "WAITING");
        s.setWaitlistPendingCount(waitlistMapper.selectCount(waitW).intValue());

        return s;
    }

    // ===== 患者轨迹查询 =====

    public List<PatientDailyMetric> getPatientTrajectory(Long patientId, LocalDateTime from, LocalDateTime to) {
        LambdaQueryWrapper<PatientDailyMetric> w = new LambdaQueryWrapper<>();
        w.eq(PatientDailyMetric::getPatientId, patientId)
                .between(PatientDailyMetric::getMetricAt, from, to)
                .orderByAsc(PatientDailyMetric::getMetricAt);
        return metricMapper.selectList(w);
    }

    public List<DoctorWorkloadSnapshot> getDoctorWorkloadTrend(Long doctorId, LocalDateTime from, LocalDateTime to) {
        LambdaQueryWrapper<DoctorWorkloadSnapshot> w = new LambdaQueryWrapper<>();
        w.eq(DoctorWorkloadSnapshot::getDoctorId, doctorId)
                .between(DoctorWorkloadSnapshot::getSnapshotAt, from, to)
                .orderByAsc(DoctorWorkloadSnapshot::getSnapshotAt);
        return workloadMapper.selectList(w);
    }

    // ===== 手动触发 =====

    public Long triggerAggregation(String scopeCode, LocalDate targetDate, Long triggeredBy) {
        Long jobId = startJob("MANUAL_TRIGGER", scopeCode, targetDate);
        try {
            int count = switch (scopeCode) {
                case "PATIENT_DAILY" -> aggregatePatientDailyMetrics(targetDate);
                case "DOCTOR_DAILY" -> aggregateDoctorWorkloads(targetDate);
                default -> 0;
            };
            finishJob(jobId, "SUCCESS", count, null);
            return jobId;
        } catch (Exception e) {
            finishJob(jobId, "FAILED", 0, e.getMessage());
            throw new RuntimeException("聚合失败: " + e.getMessage());
        }
    }

    // ===== 作业管理 =====

    private Long startJob(String jobCode, String scopeCode, LocalDate targetDate) {
        AnalyticsJobLog job = new AnalyticsJobLog();
        job.setJobCode(jobCode);
        job.setJobScopeCode(scopeCode);
        job.setJobStatus("RUNNING");
        job.setTargetDate(targetDate.atStartOfDay());
        job.setStartedAt(LocalDateTime.now());
        job.setRecordsProcessed(0);
        jobLogMapper.insert(job);
        return job.getId();
    }

    private void finishJob(Long jobId, String status, int recordsProcessed, String error) {
        AnalyticsJobLog job = jobLogMapper.selectById(jobId);
        if (job == null) return;
        job.setJobStatus(status);
        job.setFinishedAt(LocalDateTime.now());
        job.setRecordsProcessed(recordsProcessed);
        job.setErrorMessage(error);
        jobLogMapper.updateById(job);
    }

    // ===== 工具方法 =====

    private <T> BigDecimal avgOf(List<T> list, java.util.function.Function<T, Integer> getter) {
        double avg = list.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0);
        return BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);
    }
}
