package com.mental.health.service;

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
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    @Autowired private OutcomeSnapshotMapper snapshotMapper;
    @Autowired private DoctorPerformanceMapper performanceMapper;
    @Autowired private MoodDiaryMapper moodDiaryMapper;
    @Autowired private AssessmentSessionMapper sessionMapper;
    @Autowired private AppointmentMapper appointmentMapper;
    @Autowired private MeditationSessionMapper meditationSessionMapper;
    @Autowired private TreatmentPlanMapper planMapper;
    @Autowired private TreatmentGoalMapper goalMapper;
    @Autowired private RiskEventMapper riskEventMapper;
    @Autowired private CrisisAlertMapper crisisAlertMapper;
    @Autowired private SessionNoteMapper noteMapper;
    @Autowired private UserMapper userMapper;

    // ===== Patient Outcome =====

    public List<OutcomeSnapshot> getPatientOutcomeHistory(Long patientId) {
        return snapshotMapper.selectList(
                new LambdaQueryWrapper<OutcomeSnapshot>()
                        .eq(OutcomeSnapshot::getPatientId, patientId)
                        .orderByDesc(OutcomeSnapshot::getSnapshotDate));
    }

    public Map<String, Object> getPatientOutcomeSummary(Long patientId) {
        Map<String, Object> summary = new HashMap<>();

        // Latest snapshot
        List<OutcomeSnapshot> snapshots = snapshotMapper.selectList(
                new LambdaQueryWrapper<OutcomeSnapshot>()
                        .eq(OutcomeSnapshot::getPatientId, patientId)
                        .orderByDesc(OutcomeSnapshot::getSnapshotDate)
                        .last("LIMIT 2"));

        if (!snapshots.isEmpty()) {
            OutcomeSnapshot latest = snapshots.get(0);
            summary.put("latest", latest);

            if (snapshots.size() >= 2) {
                OutcomeSnapshot prev = snapshots.get(1);
                // Calculate trends
                Map<String, Object> trends = new HashMap<>();
                trends.put("moodTrend", calcTrend(latest.getAvgMoodScore(), prev.getAvgMoodScore()));
                trends.put("phq9Trend", calcIntTrend(latest.getPhq9Score(), prev.getPhq9Score()));
                trends.put("gad7Trend", calcIntTrend(latest.getGad7Score(), prev.getGad7Score()));
                trends.put("sleepTrend", calcTrend(latest.getAvgSleepQuality(), prev.getAvgSleepQuality()));
                summary.put("trends", trends);
            }
        }

        // All-time engagement stats
        long totalDiaries = moodDiaryMapper.selectCount(
                new LambdaQueryWrapper<MoodDiary>().eq(MoodDiary::getUserId, patientId));
        long totalAssessments = sessionMapper.selectCount(
                new LambdaQueryWrapper<AssessmentSession>()
                        .eq(AssessmentSession::getUserId, patientId)
                        .eq(AssessmentSession::getSessionStatus, "COMPLETED"));
        summary.put("totalDiaries", totalDiaries);
        summary.put("totalAssessments", totalAssessments);

        return summary;
    }

    @Transactional
    public OutcomeSnapshot generateSnapshot(Long patientId, Long doctorId, String snapshotType) {
        LocalDate now = LocalDate.now();
        LocalDate periodStart = switch (snapshotType) {
            case "WEEKLY" -> now.minusWeeks(1);
            case "QUARTERLY" -> now.minusMonths(3);
            default -> now.minusMonths(1);
        };

        OutcomeSnapshot snapshot = new OutcomeSnapshot();
        snapshot.setPatientId(patientId);
        snapshot.setDoctorId(doctorId);
        snapshot.setSnapshotDate(now);
        snapshot.setSnapshotType(snapshotType);

        // Mood metrics
        List<MoodDiary> diaries = moodDiaryMapper.selectList(
                new LambdaQueryWrapper<MoodDiary>()
                        .eq(MoodDiary::getUserId, patientId)
                        .ge(MoodDiary::getCreateTime, periodStart.atStartOfDay()));

        snapshot.setDiaryCount(diaries.size());
        if (!diaries.isEmpty()) {
            snapshot.setAvgMoodScore(avg(diaries.stream().map(MoodDiary::getMoodScore).collect(Collectors.toList())));
            snapshot.setAvgSleepQuality(avg(diaries.stream().map(MoodDiary::getSleepQuality).collect(Collectors.toList())));
            snapshot.setAvgStressLevel(avg(diaries.stream().map(MoodDiary::getStressLevel).collect(Collectors.toList())));
            snapshot.setAvgEnergyLevel(avg(diaries.stream().map(MoodDiary::getEnergyLevel).collect(Collectors.toList())));
        }

        // Latest assessment scores
        List<AssessmentSession> phq9Sessions = sessionMapper.selectList(
                new LambdaQueryWrapper<AssessmentSession>()
                        .eq(AssessmentSession::getUserId, patientId)
                        .eq(AssessmentSession::getSessionStatus, "COMPLETED")
                        .orderByDesc(AssessmentSession::getSubmittedAt)
                        .last("LIMIT 1"));
        if (!phq9Sessions.isEmpty()) {
            AssessmentSession s = phq9Sessions.get(0);
            snapshot.setPhq9Score(s.getTotalScore());
            snapshot.setPhq9Severity(s.getSeverityLevel());
        }

        // Appointment metrics
        long aptCount = appointmentMapper.selectCount(
                new LambdaQueryWrapper<Appointment>()
                        .eq(Appointment::getPatientId, patientId)
                        .ge(Appointment::getAppointmentTime, periodStart.atStartOfDay()));
        long aptAttended = appointmentMapper.selectCount(
                new LambdaQueryWrapper<Appointment>()
                        .eq(Appointment::getPatientId, patientId)
                        .eq(Appointment::getStatus, 2)
                        .ge(Appointment::getAppointmentTime, periodStart.atStartOfDay()));
        snapshot.setAppointmentCount((int) aptCount);
        snapshot.setAppointmentAttended((int) aptAttended);

        // Meditation
        long meditationSecs = 0;
        List<MeditationSession> medSessions = meditationSessionMapper.selectList(
                new LambdaQueryWrapper<MeditationSession>()
                        .eq(MeditationSession::getUserId, patientId)
                        .eq(MeditationSession::getSessionStatus, "COMPLETED")
                        .ge(MeditationSession::getStartedAt, periodStart.atStartOfDay()));
        for (MeditationSession ms : medSessions) {
            if (ms.getActualSeconds() != null) meditationSecs += ms.getActualSeconds();
        }
        snapshot.setMeditationMinutes((int) (meditationSecs / 60));

        // Assessment count
        long assessCount = sessionMapper.selectCount(
                new LambdaQueryWrapper<AssessmentSession>()
                        .eq(AssessmentSession::getUserId, patientId)
                        .eq(AssessmentSession::getSessionStatus, "COMPLETED")
                        .ge(AssessmentSession::getSubmittedAt, periodStart.atStartOfDay()));
        snapshot.setAssessmentCount((int) assessCount);

        // Treatment plan
        List<TreatmentPlan> activePlans = planMapper.selectList(
                new LambdaQueryWrapper<TreatmentPlan>()
                        .eq(TreatmentPlan::getPatientId, patientId)
                        .eq(TreatmentPlan::getPlanStatus, "ACTIVE"));
        if (!activePlans.isEmpty()) {
            TreatmentPlan plan = activePlans.get(0);
            snapshot.setActivePlanId(plan.getId());

            List<TreatmentGoal> goals = goalMapper.selectList(
                    new LambdaQueryWrapper<TreatmentGoal>()
                            .eq(TreatmentGoal::getPlanId, plan.getId()));
            snapshot.setGoalsTotal(goals.size());
            snapshot.setGoalsAchieved((int) goals.stream().filter(g -> "ACHIEVED".equals(g.getStatus())).count());
            snapshot.setAvgGoalProgress(goals.isEmpty() ? 0 :
                    goals.stream().mapToInt(g -> g.getProgressPct() != null ? g.getProgressPct() : 0).sum() / goals.size());
        }

        // Risk
        long riskCount = riskEventMapper.selectCount(
                new LambdaQueryWrapper<RiskEvent>()
                        .eq(RiskEvent::getUserId, patientId)
                        .ge(RiskEvent::getDetectedAt, periodStart.atStartOfDay()));
        long crisisCount = crisisAlertMapper.selectCount(
                new LambdaQueryWrapper<CrisisAlert>()
                        .eq(CrisisAlert::getUserId, patientId)
                        .ge(CrisisAlert::getCreateTime, periodStart.atStartOfDay()));
        snapshot.setRiskEventsCount((int) riskCount);
        snapshot.setCrisisAlertsCount((int) crisisCount);

        snapshotMapper.insert(snapshot);
        return snapshot;
    }

    // ===== Doctor Performance =====

    public Map<String, Object> getDoctorPerformanceSummary(Long doctorId) {
        Map<String, Object> result = new HashMap<>();

        // Current period stats
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        LocalDate monthEnd = LocalDate.now();

        long totalPatients = appointmentMapper.selectCount(
                new LambdaQueryWrapper<Appointment>()
                        .eq(Appointment::getDoctorId, doctorId)
                        .ge(Appointment::getAppointmentTime, monthStart.atStartOfDay()))
                / Math.max(1, 1); // unique patients approximation
        long completedApts = appointmentMapper.selectCount(
                new LambdaQueryWrapper<Appointment>()
                        .eq(Appointment::getDoctorId, doctorId)
                        .eq(Appointment::getStatus, 2)
                        .ge(Appointment::getAppointmentTime, monthStart.atStartOfDay()));
        long activePlans = planMapper.selectCount(
                new LambdaQueryWrapper<TreatmentPlan>()
                        .eq(TreatmentPlan::getDoctorId, doctorId)
                        .eq(TreatmentPlan::getPlanStatus, "ACTIVE"));
        long sessionNotes = noteMapper.selectCount(
                new LambdaQueryWrapper<SessionNote>()
                        .eq(SessionNote::getDoctorId, doctorId)
                        .ge(SessionNote::getSessionDate, monthStart));
        long crisisHandled = crisisAlertMapper.selectCount(
                new LambdaQueryWrapper<CrisisAlert>()
                        .eq(CrisisAlert::getDoctorId, doctorId)
                        .eq(CrisisAlert::getAlertStatus, "RESOLVED")
                        .ge(CrisisAlert::getCreateTime, monthStart.atStartOfDay()));

        result.put("completedAppointments", completedApts);
        result.put("activePlans", activePlans);
        result.put("sessionNotes", sessionNotes);
        result.put("crisisHandled", crisisHandled);
        result.put("periodStart", monthStart);
        result.put("periodEnd", monthEnd);

        // Historical performance
        List<DoctorPerformance> history = performanceMapper.selectList(
                new LambdaQueryWrapper<DoctorPerformance>()
                        .eq(DoctorPerformance::getDoctorId, doctorId)
                        .orderByDesc(DoctorPerformance::getPeriodStart)
                        .last("LIMIT 6"));
        result.put("history", history);

        return result;
    }

    // ===== System-wide Analytics (Admin) =====

    public Map<String, Object> getSystemAnalytics() {
        Map<String, Object> analytics = new HashMap<>();
        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);

        // User counts
        long totalPatients = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getRole, "PATIENT").eq(User::getStatus, 1));
        long totalDoctors = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getRole, "DOCTOR").eq(User::getStatus, 1));
        analytics.put("totalPatients", totalPatients);
        analytics.put("totalDoctors", totalDoctors);

        // Monthly appointments
        long monthlyApts = appointmentMapper.selectCount(
                new LambdaQueryWrapper<Appointment>()
                        .ge(Appointment::getAppointmentTime, monthStart.atStartOfDay()));
        long monthlyCompleted = appointmentMapper.selectCount(
                new LambdaQueryWrapper<Appointment>()
                        .eq(Appointment::getStatus, 2)
                        .ge(Appointment::getAppointmentTime, monthStart.atStartOfDay()));
        analytics.put("monthlyAppointments", monthlyApts);
        analytics.put("monthlyCompletedAppointments", monthlyCompleted);

        // Monthly assessments
        long monthlyAssess = sessionMapper.selectCount(
                new LambdaQueryWrapper<AssessmentSession>()
                        .eq(AssessmentSession::getSessionStatus, "COMPLETED")
                        .ge(AssessmentSession::getSubmittedAt, monthStart.atStartOfDay()));
        analytics.put("monthlyAssessments", monthlyAssess);

        // Active treatment plans
        long activePlans = planMapper.selectCount(
                new LambdaQueryWrapper<TreatmentPlan>()
                        .eq(TreatmentPlan::getPlanStatus, "ACTIVE"));
        analytics.put("activeTreatmentPlans", activePlans);

        // Monthly crisis alerts
        long monthlyCrisis = crisisAlertMapper.selectCount(
                new LambdaQueryWrapper<CrisisAlert>()
                        .ge(CrisisAlert::getCreateTime, monthStart.atStartOfDay()));
        long resolvedCrisis = crisisAlertMapper.selectCount(
                new LambdaQueryWrapper<CrisisAlert>()
                        .eq(CrisisAlert::getAlertStatus, "RESOLVED")
                        .ge(CrisisAlert::getCreateTime, monthStart.atStartOfDay()));
        analytics.put("monthlyCrisisAlerts", monthlyCrisis);
        analytics.put("resolvedCrisisAlerts", resolvedCrisis);

        return analytics;
    }

    // ===== Helpers =====

    private BigDecimal avg(List<Integer> values) {
        if (values == null || values.isEmpty()) return BigDecimal.ZERO;
        double sum = values.stream().filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
        long count = values.stream().filter(Objects::nonNull).count();
        if (count == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(sum / count).setScale(2, RoundingMode.HALF_UP);
    }

    private String calcTrend(BigDecimal current, BigDecimal previous) {
        if (current == null || previous == null) return "STABLE";
        int cmp = current.compareTo(previous);
        return cmp > 0 ? "UP" : cmp < 0 ? "DOWN" : "STABLE";
    }

    private String calcIntTrend(Integer current, Integer previous) {
        if (current == null || previous == null) return "STABLE";
        return current > previous ? "UP" : current < previous ? "DOWN" : "STABLE";
    }
}
