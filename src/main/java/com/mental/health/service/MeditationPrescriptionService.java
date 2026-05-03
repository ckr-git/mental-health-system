package com.mental.health.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mental.health.entity.*;
import com.mental.health.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 冥想处方与疗效追踪服务
 *
 * 处方状态机: DRAFT → ACTIVE → PAUSED → COMPLETED / CANCELLED
 *
 * 核心能力:
 * 1. 医生开冥想处方（目标导向：焦虑缓解/睡眠支持/接地/维持）
 * 2. 患者冥想时记录前后心情/压力变化
 * 3. 疗效日志汇总分析
 * 4. 与治疗计划联动（处方关联计划和阶段）
 */
@Service
public class MeditationPrescriptionService {

    private static final Logger log = LoggerFactory.getLogger(MeditationPrescriptionService.class);

    @Autowired private MeditationPrescriptionMapper prescriptionMapper;
    @Autowired private MeditationEffectLogMapper effectLogMapper;
    @Autowired private MeditationSessionMapper sessionMapper;

    // ===== 处方管理 =====

    @Transactional
    public Long createPrescription(Long patientId, Long doctorId, Long exerciseId,
                                    String goalCode, String frequencyCode,
                                    Integer sessionsPerWeek, Integer minutesPerSession,
                                    Long treatmentPlanId) {
        MeditationPrescription p = new MeditationPrescription();
        p.setPatientId(patientId);
        p.setDoctorId(doctorId);
        p.setExerciseId(exerciseId);
        p.setPrescriptionStatus("DRAFT");
        p.setGoalCode(goalCode);
        p.setFrequencyCode(frequencyCode != null ? frequencyCode : "DAILY");
        p.setSessionsPerWeek(sessionsPerWeek != null ? sessionsPerWeek : 3);
        p.setMinutesPerSession(minutesPerSession != null ? minutesPerSession : 10);
        p.setStartAt(LocalDateTime.now());
        p.setTreatmentPlanId(treatmentPlanId);
        p.setTotalSessionsCompleted(0);
        prescriptionMapper.insert(p);
        return p.getId();
    }

    @Transactional
    public void activatePrescription(Long prescriptionId) {
        MeditationPrescription p = prescriptionMapper.selectById(prescriptionId);
        if (p == null || !"DRAFT".equals(p.getPrescriptionStatus()))
            throw new RuntimeException("处方不存在或不可激活");
        p.setPrescriptionStatus("ACTIVE");
        p.setStartAt(LocalDateTime.now());
        p.setNextDueAt(LocalDateTime.now().plusDays(1));
        prescriptionMapper.updateById(p);
    }

    @Transactional
    public void pausePrescription(Long prescriptionId, String reason) {
        MeditationPrescription p = prescriptionMapper.selectById(prescriptionId);
        if (p == null || !"ACTIVE".equals(p.getPrescriptionStatus())) return;
        p.setPrescriptionStatus("PAUSED");
        prescriptionMapper.updateById(p);
    }

    @Transactional
    public void completePrescription(Long prescriptionId) {
        MeditationPrescription p = prescriptionMapper.selectById(prescriptionId);
        if (p == null) return;
        p.setPrescriptionStatus("COMPLETED");
        p.setEndAt(LocalDateTime.now());
        prescriptionMapper.updateById(p);
    }

    public List<MeditationPrescription> getPatientPrescriptions(Long patientId, String status) {
        LambdaQueryWrapper<MeditationPrescription> w = new LambdaQueryWrapper<>();
        w.eq(MeditationPrescription::getPatientId, patientId);
        if (status != null) w.eq(MeditationPrescription::getPrescriptionStatus, status);
        w.orderByDesc(MeditationPrescription::getCreateTime);
        return prescriptionMapper.selectList(w);
    }

    public List<MeditationPrescription> getDoctorPrescriptions(Long doctorId, String status) {
        LambdaQueryWrapper<MeditationPrescription> w = new LambdaQueryWrapper<>();
        w.eq(MeditationPrescription::getDoctorId, doctorId);
        if (status != null) w.eq(MeditationPrescription::getPrescriptionStatus, status);
        w.orderByDesc(MeditationPrescription::getCreateTime);
        return prescriptionMapper.selectList(w);
    }

    // ===== 疗效记录 =====

    @Transactional
    public Long recordEffect(Long sessionId, Long prescriptionId, Long patientId,
                              Integer preMood, Integer postMood, Integer preStress, Integer postStress,
                              Integer perceivedBenefit, String note) {
        MeditationEffectLog effect = new MeditationEffectLog();
        effect.setMeditationSessionId(sessionId);
        effect.setMeditationPrescriptionId(prescriptionId);
        effect.setPatientId(patientId);
        effect.setEffectStatus("RECORDED");
        effect.setPreMoodScore(preMood);
        effect.setPostMoodScore(postMood);
        effect.setPreStressScore(preStress);
        effect.setPostStressScore(postStress);
        effect.setMoodChange(postMood != null && preMood != null ? postMood - preMood : null);
        effect.setStressChange(postStress != null && preStress != null ? postStress - preStress : null);
        effect.setPerceivedBenefitScore(perceivedBenefit);
        effect.setNote(note);
        effect.setRecordedAt(LocalDateTime.now());
        effectLogMapper.insert(effect);

        // 更新处方统计
        if (prescriptionId != null) {
            MeditationPrescription p = prescriptionMapper.selectById(prescriptionId);
            if (p != null) {
                p.setTotalSessionsCompleted(p.getTotalSessionsCompleted() + 1);
                // 更新平均效果评分
                if (perceivedBenefit != null) {
                    BigDecimal oldAvg = p.getAvgEffectScore();
                    int total = p.getTotalSessionsCompleted();
                    if (oldAvg == null) {
                        p.setAvgEffectScore(BigDecimal.valueOf(perceivedBenefit));
                    } else {
                        BigDecimal newAvg = oldAvg.multiply(BigDecimal.valueOf(total - 1))
                                .add(BigDecimal.valueOf(perceivedBenefit))
                                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
                        p.setAvgEffectScore(newAvg);
                    }
                }
                // 设置下次建议时间
                p.setNextDueAt(LocalDateTime.now().plusDays(
                        "DAILY".equals(p.getFrequencyCode()) ? 1 : 7 / Math.max(p.getSessionsPerWeek(), 1)));
                prescriptionMapper.updateById(p);
            }
        }

        return effect.getId();
    }

    // ===== 疗效分析 =====

    public Map<String, Object> summarizeEffect(Long patientId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);

        LambdaQueryWrapper<MeditationEffectLog> w = new LambdaQueryWrapper<>();
        w.eq(MeditationEffectLog::getPatientId, patientId)
                .ge(MeditationEffectLog::getRecordedAt, since)
                .orderByAsc(MeditationEffectLog::getRecordedAt);
        List<MeditationEffectLog> effects = effectLogMapper.selectList(w);

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalSessions", effects.size());
        summary.put("period", days + "天");

        if (effects.isEmpty()) return summary;

        // 平均心情变化
        double avgMoodChange = effects.stream()
                .filter(e -> e.getMoodChange() != null)
                .mapToInt(MeditationEffectLog::getMoodChange)
                .average().orElse(0);
        summary.put("avgMoodChange", BigDecimal.valueOf(avgMoodChange).setScale(2, RoundingMode.HALF_UP));

        // 平均压力变化
        double avgStressChange = effects.stream()
                .filter(e -> e.getStressChange() != null)
                .mapToInt(MeditationEffectLog::getStressChange)
                .average().orElse(0);
        summary.put("avgStressChange", BigDecimal.valueOf(avgStressChange).setScale(2, RoundingMode.HALF_UP));

        // 平均自评收益
        double avgBenefit = effects.stream()
                .filter(e -> e.getPerceivedBenefitScore() != null)
                .mapToInt(MeditationEffectLog::getPerceivedBenefitScore)
                .average().orElse(0);
        summary.put("avgPerceivedBenefit", BigDecimal.valueOf(avgBenefit).setScale(2, RoundingMode.HALF_UP));

        // 有效率（心情改善的比例）
        long improved = effects.stream()
                .filter(e -> e.getMoodChange() != null && e.getMoodChange() > 0)
                .count();
        summary.put("improvementRate", effects.isEmpty() ? 0 :
                BigDecimal.valueOf((double) improved / effects.size() * 100).setScale(1, RoundingMode.HALF_UP));

        // 趋势数据
        List<Map<String, Object>> trend = new ArrayList<>();
        for (MeditationEffectLog e : effects) {
            Map<String, Object> point = new HashMap<>();
            point.put("date", e.getRecordedAt());
            point.put("moodChange", e.getMoodChange());
            point.put("stressChange", e.getStressChange());
            point.put("benefit", e.getPerceivedBenefitScore());
            trend.add(point);
        }
        summary.put("trend", trend);

        return summary;
    }

    public List<MeditationEffectLog> getPatientEffectLogs(Long patientId, int limit) {
        LambdaQueryWrapper<MeditationEffectLog> w = new LambdaQueryWrapper<>();
        w.eq(MeditationEffectLog::getPatientId, patientId)
                .orderByDesc(MeditationEffectLog::getRecordedAt)
                .last("LIMIT " + limit);
        return effectLogMapper.selectList(w);
    }
}
