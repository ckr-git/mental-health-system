package com.mental.health.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mental.health.dto.CreateNotificationCommand;
import com.mental.health.entity.*;
import com.mental.health.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 危机案例服务 — 核心状态机与闭环逻辑
 *
 * 状态流转:
 * NEW → TRIAGED → ACKED → CONTACTING → INTERVENING → STABILIZED → RESOLVED → POST_REVIEW
 *       ↗ ESCALATED (任意非终态均可升级)
 */
@Service
public class CrisisCaseService {

    private static final Logger log = LoggerFactory.getLogger(CrisisCaseService.class);

    // SLA: triage_level → 响应截止分钟数
    private static final Map<String, Integer> SLA_MINUTES = Map.of(
            "CRITICAL", 15,
            "HIGH", 60,
            "MEDIUM", 240
    );

    // 合法的状态转换
    private static final Map<String, Set<String>> VALID_TRANSITIONS = Map.ofEntries(
            Map.entry("NEW", Set.of("TRIAGED", "ESCALATED")),
            Map.entry("TRIAGED", Set.of("ACKED", "ESCALATED")),
            Map.entry("ACKED", Set.of("CONTACTING", "ESCALATED")),
            Map.entry("CONTACTING", Set.of("INTERVENING", "ESCALATED")),
            Map.entry("INTERVENING", Set.of("STABILIZED", "ESCALATED")),
            Map.entry("ESCALATED", Set.of("ACKED", "CONTACTING", "INTERVENING", "STABILIZED")),
            Map.entry("STABILIZED", Set.of("RESOLVED")),
            Map.entry("RESOLVED", Set.of("POST_REVIEW"))
    );

    @Autowired private CrisisCaseMapper caseMapper;
    @Autowired private CrisisEscalationStepMapper escalationStepMapper;
    @Autowired private CrisisContactAttemptMapper contactAttemptMapper;
    @Autowired private PatientSafetyPlanMapper safetyPlanMapper;
    @Autowired private RiskEventMapper riskEventMapper;
    @Autowired private CrisisAlertMapper crisisAlertMapper;
    @Autowired private PatientDoctorRelationshipMapper relationshipMapper;
    @Autowired private OutboxService outboxService;
    @Autowired private UserNotificationService notificationService;

    // ===== 案例创建 =====

    /**
     * 从风险事件创建或合并危机案例
     * 72小时内同一患者的事件会合并到已有案例
     */
    @Transactional
    public CrisisCase openOrMergeCase(Long patientId, Long riskEventId, String triageLevel, String source, String title) {
        // 查找72小时内未关闭的案例
        CrisisCase existingCase = caseMapper.findOpenCaseByPatient(patientId);

        if (existingCase != null) {
            // 合并到已有案例
            existingCase.setMergedEventCount(existingCase.getMergedEventCount() + 1);
            // 升级等级（取更高）
            if (compareSeverity(triageLevel, existingCase.getTriageLevel()) > 0) {
                existingCase.setTriageLevel(triageLevel);
                existingCase.setSlaDeadline(calculateSlaDeadline(triageLevel));
            }
            caseMapper.updateById(existingCase);

            // 关联风险事件
            linkRiskEventToCase(riskEventId, existingCase.getId());

            log.info("危机事件合并到案例#{}, 患者#{}, 累计{}个事件",
                    existingCase.getId(), patientId, existingCase.getMergedEventCount());
            return existingCase;
        }

        // 创建新案例
        CrisisCase newCase = new CrisisCase();
        newCase.setPatientId(patientId);
        newCase.setCaseStatus("NEW");
        newCase.setTriageLevel(triageLevel);
        newCase.setCaseSource(source);
        newCase.setTitle(title);
        newCase.setInitialRiskEventId(riskEventId);
        newCase.setMergedEventCount(1);
        newCase.setReopenCount(0);
        newCase.setOpenedAt(LocalDateTime.now());
        newCase.setSlaDeadline(calculateSlaDeadline(triageLevel));

        // 自动分配主责医生
        Long doctorId = findResponsibleDoctor(patientId);
        newCase.setOwnerDoctorId(doctorId);

        // 构建升级链
        newCase.setEscalationChainJson(buildDefaultEscalationChain(triageLevel));

        caseMapper.insert(newCase);

        // 关联风险事件
        linkRiskEventToCase(riskEventId, newCase.getId());

        // 发送危机通知
        notifyCrisisOpened(newCase);

        // 发布事件
        publishCrisisEvent(newCase, "CRISIS_CASE_OPENED");

        log.info("新危机案例#{} 已创建, 患者#{}, 等级={}", newCase.getId(), patientId, triageLevel);
        return newCase;
    }

    // ===== 状态机转换 =====

    @Transactional
    public void transitionStatus(Long caseId, String targetStatus, Long operatorId, String note) {
        CrisisCase crisisCase = caseMapper.selectById(caseId);
        if (crisisCase == null) {
            throw new RuntimeException("危机案例不存在");
        }

        String currentStatus = crisisCase.getCaseStatus();
        Set<String> validTargets = VALID_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!validTargets.contains(targetStatus)) {
            throw new RuntimeException("无效的状态转换: " + currentStatus + " → " + targetStatus);
        }

        String oldStatus = currentStatus;
        crisisCase.setCaseStatus(targetStatus);

        // 根据目标状态设置时间戳
        LocalDateTime now = LocalDateTime.now();
        switch (targetStatus) {
            case "TRIAGED" -> {
                crisisCase.setTriagedAt(now);
                crisisCase.setTriagedBy(operatorId);
            }
            case "ACKED" -> {
                // 确认后更新下一步行动截止
                crisisCase.setNextActionDeadline(now.plusHours(
                        "CRITICAL".equals(crisisCase.getTriageLevel()) ? 1 : 4
                ));
            }
            case "RESOLVED" -> {
                crisisCase.setResolvedAt(now);
                crisisCase.setResolvedBy(operatorId);
                crisisCase.setResolutionNote(note);
                // 设置复盘截止（3天后）
                crisisCase.setPostReviewDueAt(now.plusDays(3));
            }
            case "ESCALATED" -> {
                recordEscalationStep(crisisCase, "MANUAL", oldStatus, targetStatus, operatorId, note);
            }
            case "POST_REVIEW" -> {
                crisisCase.setPostReviewCompletedAt(now);
            }
        }

        caseMapper.updateById(crisisCase);

        // 记录操作日志
        recordCaseAction(caseId, oldStatus, targetStatus, operatorId, note);

        // 发布事件
        publishCrisisEvent(crisisCase, "CRISIS_STATUS_CHANGED");

        log.info("危机案例#{} 状态转换: {} → {}, 操作人#{}", caseId, oldStatus, targetStatus, operatorId);
    }

    // ===== 分诊 =====

    @Transactional
    public void triageCase(Long caseId, Long operatorId, String triageLevel, Long assignDoctorId, String note) {
        CrisisCase crisisCase = caseMapper.selectById(caseId);
        if (crisisCase == null || !"NEW".equals(crisisCase.getCaseStatus())) {
            throw new RuntimeException("只能分诊NEW状态的案例");
        }

        crisisCase.setTriageLevel(triageLevel);
        crisisCase.setSlaDeadline(calculateSlaDeadline(triageLevel));
        if (assignDoctorId != null) {
            crisisCase.setOwnerDoctorId(assignDoctorId);
        }

        transitionStatus(caseId, "TRIAGED", operatorId, note);
    }

    // ===== 重新打开 =====

    @Transactional
    public void reopenCase(Long caseId, Long newRiskEventId, String reason) {
        CrisisCase crisisCase = caseMapper.selectById(caseId);
        if (crisisCase == null || !"RESOLVED".equals(crisisCase.getCaseStatus())) {
            throw new RuntimeException("只能重新打开已解决的案例");
        }

        crisisCase.setCaseStatus("NEW");
        crisisCase.setReopenCount(crisisCase.getReopenCount() + 1);
        crisisCase.setResolvedAt(null);
        crisisCase.setResolvedBy(null);
        crisisCase.setResolutionCode(null);
        crisisCase.setResolutionNote(null);
        crisisCase.setOpenedAt(LocalDateTime.now());
        crisisCase.setSlaDeadline(calculateSlaDeadline(crisisCase.getTriageLevel()));

        if (newRiskEventId != null) {
            crisisCase.setMergedEventCount(crisisCase.getMergedEventCount() + 1);
            linkRiskEventToCase(newRiskEventId, caseId);
        }

        caseMapper.updateById(crisisCase);

        recordEscalationStep(crisisCase, "REOPEN", "RESOLVED", "NEW", null, reason);
        notifyCrisisOpened(crisisCase);
        publishCrisisEvent(crisisCase, "CRISIS_CASE_REOPENED");

        log.warn("危机案例#{} 被重新打开 (第{}次), 原因: {}", caseId, crisisCase.getReopenCount(), reason);
    }

    // ===== 联系尝试 =====

    @Transactional
    public void recordContactAttempt(Long caseId, String target, String channel,
                                      String contactName, String contactInfo,
                                      String status, Long operatorId, String note) {
        CrisisContactAttempt attempt = new CrisisContactAttempt();
        attempt.setCrisisCaseId(caseId);
        attempt.setContactTarget(target);
        attempt.setContactChannel(channel);
        attempt.setContactName(contactName);
        attempt.setContactInfo(contactInfo);
        attempt.setAttemptStatus(status);
        attempt.setAttemptedAt(LocalDateTime.now());
        attempt.setAttemptedBy(operatorId);
        attempt.setOutcomeNote(note);
        contactAttemptMapper.insert(attempt);
    }

    // ===== 安全计划 =====

    @Transactional
    public Long createSafetyPlan(Long patientId, Long doctorId, PatientSafetyPlan plan) {
        plan.setPatientId(patientId);
        plan.setCreatedBy(doctorId);
        plan.setPlanStatus("ACTIVE");

        // 归档旧的安全计划
        LambdaQueryWrapper<PatientSafetyPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PatientSafetyPlan::getPatientId, patientId)
                .eq(PatientSafetyPlan::getPlanStatus, "ACTIVE");
        PatientSafetyPlan existing = safetyPlanMapper.selectOne(wrapper);
        if (existing != null) {
            existing.setPlanStatus("ARCHIVED");
            safetyPlanMapper.updateById(existing);
        }

        safetyPlanMapper.insert(plan);
        return plan.getId();
    }

    public PatientSafetyPlan getActiveSafetyPlan(Long patientId) {
        LambdaQueryWrapper<PatientSafetyPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PatientSafetyPlan::getPatientId, patientId)
                .eq(PatientSafetyPlan::getPlanStatus, "ACTIVE")
                .last("LIMIT 1");
        return safetyPlanMapper.selectOne(wrapper);
    }

    // ===== 查询 =====

    public Page<CrisisCase> getDoctorCases(Long doctorId, String status, int pageNum, int pageSize) {
        Page<CrisisCase> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CrisisCase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CrisisCase::getOwnerDoctorId, doctorId);
        if (status != null) {
            wrapper.eq(CrisisCase::getCaseStatus, status);
        } else {
            wrapper.notIn(CrisisCase::getCaseStatus, "RESOLVED", "POST_REVIEW");
        }
        wrapper.orderByAsc(CrisisCase::getSlaDeadline);
        return caseMapper.selectPage(page, wrapper);
    }

    public Page<CrisisCase> getPatientCases(Long patientId, int pageNum, int pageSize) {
        Page<CrisisCase> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CrisisCase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CrisisCase::getPatientId, patientId)
                .orderByDesc(CrisisCase::getOpenedAt);
        return caseMapper.selectPage(page, wrapper);
    }

    public CrisisCase getCaseDetail(Long caseId) {
        return caseMapper.selectById(caseId);
    }

    public List<CrisisEscalationStep> getCaseEscalationSteps(Long caseId) {
        LambdaQueryWrapper<CrisisEscalationStep> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CrisisEscalationStep::getCrisisCaseId, caseId)
                .orderByAsc(CrisisEscalationStep::getStepOrder);
        return escalationStepMapper.selectList(wrapper);
    }

    public List<CrisisContactAttempt> getCaseContactAttempts(Long caseId) {
        LambdaQueryWrapper<CrisisContactAttempt> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CrisisContactAttempt::getCrisisCaseId, caseId)
                .orderByDesc(CrisisContactAttempt::getAttemptedAt);
        return contactAttemptMapper.selectList(wrapper);
    }

    // ===== 调度器：超时升级 =====

    @Transactional
    public void processOverdueCases() {
        List<CrisisCase> overdueCases = caseMapper.findOverdueCases();
        for (CrisisCase c : overdueCases) {
            try {
                String currentStatus = c.getCaseStatus();
                // 超时自动升级
                if (!"ESCALATED".equals(currentStatus)) {
                    c.setCaseStatus("ESCALATED");
                    caseMapper.updateById(c);

                    recordEscalationStep(c, "TIMEOUT", currentStatus, "ESCALATED", null,
                            "SLA超时自动升级");

                    // 通知管理员
                    notifyCrisisEscalated(c, "SLA超时");

                    publishCrisisEvent(c, "CRISIS_CASE_ESCALATED");

                    log.warn("危机案例#{} SLA超时自动升级, 原状态={}", c.getId(), currentStatus);
                }
            } catch (Exception e) {
                log.error("处理超时危机案例#{} 失败: {}", c.getId(), e.getMessage());
            }
        }
    }

    // ===== 内部方法 =====

    private void linkRiskEventToCase(Long riskEventId, Long caseId) {
        RiskEvent event = riskEventMapper.selectById(riskEventId);
        if (event != null) {
            event.setCrisisCaseId(caseId);
            riskEventMapper.updateById(event);
        }
    }

    private Long findResponsibleDoctor(Long patientId) {
        LambdaQueryWrapper<PatientDoctorRelationship> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PatientDoctorRelationship::getPatientId, patientId)
                .eq(PatientDoctorRelationship::getRelationshipStatus, "ACTIVE")
                .last("LIMIT 1");
        PatientDoctorRelationship rel = relationshipMapper.selectOne(wrapper);
        return rel != null ? rel.getDoctorId() : null;
    }

    private LocalDateTime calculateSlaDeadline(String triageLevel) {
        int minutes = SLA_MINUTES.getOrDefault(triageLevel, 240);
        return LocalDateTime.now().plusMinutes(minutes);
    }

    private int compareSeverity(String a, String b) {
        Map<String, Integer> order = Map.of("MEDIUM", 1, "HIGH", 2, "CRITICAL", 3);
        return Integer.compare(order.getOrDefault(a, 0), order.getOrDefault(b, 0));
    }

    private void recordEscalationStep(CrisisCase crisisCase, String type, String fromStatus,
                                       String toStatus, Long operatorId, String note) {
        // 计算当前步骤序号
        LambdaQueryWrapper<CrisisEscalationStep> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(CrisisEscalationStep::getCrisisCaseId, crisisCase.getId());
        long count = escalationStepMapper.selectCount(countWrapper);

        CrisisEscalationStep step = new CrisisEscalationStep();
        step.setCrisisCaseId(crisisCase.getId());
        step.setStepOrder((int) count + 1);
        step.setEscalationType(type);
        step.setFromStatus(fromStatus);
        step.setToStatus(toStatus);
        step.setExecutedAt(LocalDateTime.now());
        step.setExecutedBy(operatorId);
        step.setStepResult("SUCCESS");
        step.setNote(note);
        escalationStepMapper.insert(step);
    }

    private void recordCaseAction(Long caseId, String fromStatus, String toStatus, Long operatorId, String note) {
        CrisisAlertAction action = new CrisisAlertAction();
        action.setActionType("STATUS_CHANGE:" + fromStatus + "→" + toStatus);
        action.setOperatorId(operatorId);
        action.setOperatorRole(operatorId != null ? "DOCTOR" : "SYSTEM");
        action.setActionNote(note);
        action.setActionTime(LocalDateTime.now());
        // 使用metadata存储caseId，因为action原本关联alert
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("crisisCaseId", caseId);
        metadata.put("fromStatus", fromStatus);
        metadata.put("toStatus", toStatus);
        action.setMetadata(JSON.toJSONString(metadata));
    }

    private String buildDefaultEscalationChain(String triageLevel) {
        List<Map<String, Object>> chain = new ArrayList<>();
        switch (triageLevel) {
            case "CRITICAL" -> {
                chain.add(Map.of("step", 1, "timeout_min", 15, "target", "OWNER_DOCTOR", "action", "NOTIFY"));
                chain.add(Map.of("step", 2, "timeout_min", 30, "target", "ON_CALL_DOCTOR", "action", "ESCALATE"));
                chain.add(Map.of("step", 3, "timeout_min", 60, "target", "ADMIN", "action", "ESCALATE"));
            }
            case "HIGH" -> {
                chain.add(Map.of("step", 1, "timeout_min", 60, "target", "OWNER_DOCTOR", "action", "NOTIFY"));
                chain.add(Map.of("step", 2, "timeout_min", 120, "target", "ADMIN", "action", "ESCALATE"));
            }
            default -> {
                chain.add(Map.of("step", 1, "timeout_min", 240, "target", "OWNER_DOCTOR", "action", "NOTIFY"));
            }
        }
        return JSON.toJSONString(chain);
    }

    private void notifyCrisisOpened(CrisisCase crisisCase) {
        if (crisisCase.getOwnerDoctorId() != null) {
            try {
                CreateNotificationCommand cmd = new CreateNotificationCommand();
                cmd.setUserId(crisisCase.getOwnerDoctorId());
                cmd.setCategory("CRISIS");
                cmd.setPriority("URGENT");
                cmd.setTitle("紧急：患者#" + crisisCase.getPatientId() + "触发" + crisisCase.getTriageLevel() + "级危机");
                cmd.setContent(crisisCase.getTitle());
                cmd.setActionType("ROUTE");
                cmd.setSourceType("CRISIS_CASE");
                cmd.setSourceId(crisisCase.getId());
                notificationService.createNotification(cmd);
            } catch (Exception e) {
                log.error("发送危机通知失败: {}", e.getMessage());
            }
        }
    }

    private void notifyCrisisEscalated(CrisisCase crisisCase, String reason) {
        try {
            CreateNotificationCommand cmd = new CreateNotificationCommand();
            cmd.setCategory("CRISIS");
            cmd.setPriority("URGENT");
            cmd.setTitle("危机升级：案例#" + crisisCase.getId() + " " + reason);
            cmd.setContent("患者#" + crisisCase.getPatientId() + "的危机案例已升级，原因: " + reason);
            cmd.setActionType("ROUTE");
            cmd.setSourceType("CRISIS_CASE");
            cmd.setSourceId(crisisCase.getId());
            // 通知所有管理员
            notificationService.createNotification(cmd);
        } catch (Exception e) {
            log.error("发送升级通知失败: {}", e.getMessage());
        }
    }

    private void publishCrisisEvent(CrisisCase crisisCase, String eventType) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("caseId", crisisCase.getId());
            payload.put("patientId", crisisCase.getPatientId());
            payload.put("caseStatus", crisisCase.getCaseStatus());
            payload.put("triageLevel", crisisCase.getTriageLevel());
            payload.put("ownerDoctorId", crisisCase.getOwnerDoctorId());
            outboxService.append("CRISIS_CASE", crisisCase.getId(), eventType,
                    eventType + ":" + crisisCase.getId(), JSON.toJSONString(payload));
        } catch (Exception e) {
            log.error("发布危机事件失败: {}", e.getMessage());
        }
    }
}
