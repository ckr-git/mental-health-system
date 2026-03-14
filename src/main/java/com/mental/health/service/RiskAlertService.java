package com.mental.health.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mental.health.dto.CreateNotificationCommand;
import com.mental.health.entity.*;
import com.mental.health.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
public class RiskAlertService {

    @Autowired
    private RiskScoringService riskScoringService;

    @Autowired
    private RiskEventMapper riskEventMapper;

    @Autowired
    private CrisisAlertMapper crisisAlertMapper;

    @Autowired
    private CrisisAlertActionMapper crisisAlertActionMapper;

    @Autowired
    private UserNotificationService userNotificationService;

    @Autowired
    private CrisisPushService crisisPushService;

    @Transactional
    public void analyzeAndHandle(String sourceType, Long sourceId, Long userId,
                                  String textContent, Map<String, Object> structuredData) {
        RiskScoringService.RiskEvaluation eval = riskScoringService.evaluate(
                sourceType, userId, textContent, structuredData);

        RiskEvent event = new RiskEvent();
        event.setUserId(userId);
        event.setSourceType(sourceType);
        event.setSourceId(sourceId);
        event.setContentSnapshot(textContent != null ? textContent.substring(0, Math.min(textContent.length(), 200)) : null);
        event.setMatchedRules(JSON.toJSONString(eval.getMatchedRules()));
        event.setComputedScore(eval.getTotalScore());
        event.setComputedLevel(eval.getFinalLevel());
        event.setDetectedAt(LocalDateTime.now());

        if ("LOW".equals(eval.getFinalLevel()) || eval.getMatchedRules().isEmpty()) {
            event.setDecision("NO_ALERT");
            riskEventMapper.insert(event);
            return;
        }

        CrisisAlert existing = crisisAlertMapper.findRecentActiveByUserAndLevel(
                userId, eval.getFinalLevel(), LocalDateTime.now().minusHours(4));
        if (existing != null) {
            event.setDecision("SUPPRESSED");
            event.setSuppressedByAlertId(existing.getId());
            riskEventMapper.insert(event);
            log.info("Risk alert suppressed for user {} level {} (duplicate within 4h)", userId, eval.getFinalLevel());
            return;
        }

        event.setDecision("ALERTED");
        riskEventMapper.insert(event);

        Long doctorId = findAssignedDoctor(userId);
        CrisisAlert alert = createAlert(event, doctorId, eval);
        logAction(alert.getId(), "CREATED", null, "SYSTEM", null);

        if (doctorId != null) {
            CreateNotificationCommand cmd = new CreateNotificationCommand();
            cmd.setUserId(doctorId);
            cmd.setCategory("CRISIS");
            cmd.setPriority("URGENT");
            cmd.setTitle("风险预警：" + eval.getFinalLevel());
            cmd.setContent("患者触发" + eval.getFinalLevel() + "级风险预警，请及时处理");
            cmd.setActionType("ROUTE");
            cmd.setActionPayload("{\"route\":\"/doctor/crisis-alerts?id=" + alert.getId() + "\"}");
            cmd.setSourceType("CRISIS_ALERT");
            cmd.setSourceId(alert.getId());
            userNotificationService.createNotification(cmd);

            crisisPushService.pushCrisisAlert(doctorId, buildAlertPush(alert, eval));
        }

        if ("CRITICAL".equals(eval.getFinalLevel())) {
            CreateNotificationCommand patientCmd = new CreateNotificationCommand();
            patientCmd.setUserId(userId);
            patientCmd.setCategory("CRISIS");
            patientCmd.setPriority("URGENT");
            patientCmd.setTitle("您有可用的安全资源");
            patientCmd.setContent("如果您正在经历困扰，请拨打24小时心理援助热线：400-161-9995");
            patientCmd.setActionType("ROUTE");
            patientCmd.setActionPayload("{\"route\":\"/patient/safety-resources\"}");
            patientCmd.setSourceType("CRISIS_ALERT");
            patientCmd.setSourceId(alert.getId());
            userNotificationService.createNotification(patientCmd);
        }
    }

    @Transactional
    public void acknowledge(Long alertId, Long doctorId, String note) {
        CrisisAlert alert = crisisAlertMapper.selectById(alertId);
        if (alert == null) throw new RuntimeException("告警不存在");
        if (!"OPEN".equals(alert.getAlertStatus())) throw new RuntimeException("告警状态不允许确认");

        alert.setAlertStatus("ACKNOWLEDGED");
        alert.setAcknowledgedBy(doctorId);
        alert.setAcknowledgedAt(LocalDateTime.now());
        crisisAlertMapper.updateById(alert);
        logAction(alertId, "ACKNOWLEDGED", doctorId, "DOCTOR", note);
    }

    @Transactional
    public void resolve(Long alertId, Long operatorId, String role, String note) {
        CrisisAlert alert = crisisAlertMapper.selectById(alertId);
        if (alert == null) throw new RuntimeException("告警不存在");

        alert.setAlertStatus("RESOLVED");
        alert.setResolvedBy(operatorId);
        alert.setResolvedAt(LocalDateTime.now());
        alert.setResolutionNote(note);
        crisisAlertMapper.updateById(alert);
        logAction(alertId, "RESOLVED", operatorId, role, note);
    }

    private CrisisAlert createAlert(RiskEvent event, Long doctorId,
                                     RiskScoringService.RiskEvaluation eval) {
        CrisisAlert alert = new CrisisAlert();
        alert.setUserId(event.getUserId());
        alert.setDoctorId(doctorId);
        alert.setRiskEventId(event.getId());
        alert.setAlertLevel(eval.getFinalLevel());
        alert.setAlertStatus("OPEN");
        alert.setTitle(eval.getFinalLevel() + "级风险预警");
        alert.setSummary("检测到" + eval.getMatchedRules().size() + "条风险信号，总分" + eval.getTotalScore());
        alert.setEvidenceJson(JSON.toJSONString(eval.getMatchedRules()));
        alert.setSlaDeadline(computeDeadline(eval.getFinalLevel()));
        crisisAlertMapper.insert(alert);
        return alert;
    }

    private LocalDateTime computeDeadline(String level) {
        return switch (level) {
            case "CRITICAL" -> LocalDateTime.now().plusMinutes(30);
            case "HIGH" -> LocalDateTime.now().plusHours(4);
            case "MEDIUM" -> LocalDateTime.now().plusHours(24);
            default -> LocalDateTime.now().plusHours(24);
        };
    }

    private void logAction(Long alertId, String type, Long operatorId, String role, String note) {
        CrisisAlertAction action = new CrisisAlertAction();
        action.setAlertId(alertId);
        action.setActionType(type);
        action.setOperatorId(operatorId);
        action.setOperatorRole(role);
        action.setActionNote(note);
        action.setActionTime(LocalDateTime.now());
        crisisAlertActionMapper.insert(action);
    }

    private Long findAssignedDoctor(Long userId) {
        LambdaQueryWrapper<CrisisAlert> w = new LambdaQueryWrapper<>();
        w.eq(CrisisAlert::getUserId, userId)
                .isNotNull(CrisisAlert::getDoctorId)
                .orderByDesc(CrisisAlert::getCreateTime)
                .last("LIMIT 1");
        CrisisAlert prev = crisisAlertMapper.selectOne(w);
        return prev != null ? prev.getDoctorId() : null;
    }

    private Map<String, Object> buildAlertPush(CrisisAlert alert, RiskScoringService.RiskEvaluation eval) {
        return Map.of(
                "alertId", alert.getId(),
                "userId", alert.getUserId(),
                "alertLevel", alert.getAlertLevel(),
                "title", alert.getTitle(),
                "summary", alert.getSummary(),
                "slaDeadline", alert.getSlaDeadline().toString(),
                "matchedRules", eval.getMatchedRules().size()
        );
    }
}
