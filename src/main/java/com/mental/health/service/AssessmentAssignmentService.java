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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 评估复评服务 — 协议驱动的评估任务分配与纵向分析
 *
 * 核心能力:
 * 1. 评估协议定义: 什么事件触发什么量表
 * 2. 任务分配: 给患者分配评估任务，支持截止时间、提醒、逾期
 * 3. 基线管理: 建立/替换评估基线
 * 4. 维度评分: 按维度拆分评分，支持趋势分析
 * 5. 纵向变化: 计算相对基线的变化和临床显著性
 */
@Service
public class AssessmentAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(AssessmentAssignmentService.class);

    @Autowired private AssessmentProtocolMapper protocolMapper;
    @Autowired private AssessmentAssignmentMapper assignmentMapper;
    @Autowired private AssessmentBaselineMapper baselineMapper;
    @Autowired private AssessmentDimensionResultMapper dimensionResultMapper;
    @Autowired private AssessmentScaleMapper scaleMapper;
    @Autowired private AssessmentSessionMapper sessionMapper;
    @Autowired private UserNotificationService notificationService;
    @Autowired private OutboxService outboxService;

    // ===== 协议触发 =====

    /**
     * 根据触发条件自动分配评估任务
     */
    @Transactional
    public List<Long> triggerProtocol(String triggerCondition, Long patientId,
                                      Long assignedBy, String sourceEventType, Long sourceEventId) {
        List<AssessmentProtocol> protocols = protocolMapper.findByTrigger(triggerCondition);
        List<Long> assignmentIds = new ArrayList<>();

        for (AssessmentProtocol protocol : protocols) {
            if (!"Y".equals(protocol.getAutoAssignFlag())) continue;

            List<String> scaleCodes = JSON.parseArray(protocol.getScaleCodesJson(), String.class);
            if (scaleCodes == null) continue;

            for (String scaleCode : scaleCodes) {
                // 检查是否已有未完成的同量表任务
                if (hasPendingAssignment(patientId, scaleCode)) continue;

                AssessmentScale scale = scaleMapper.findLatestByScaleCode(scaleCode);
                if (scale == null) continue;

                Long id = createAssignment(patientId, protocol.getId(), scale.getId(), scaleCode,
                        assignedBy, protocol.getTriggerCondition().equals("ON_INTAKE"),
                        sourceEventType, sourceEventId, null);
                assignmentIds.add(id);
            }
        }
        return assignmentIds;
    }

    // ===== 手动分配 =====

    @Transactional
    public Long assignManually(Long patientId, String scaleCode, Long assignedBy,
                                LocalDateTime dueAt, boolean isBaseline, Long treatmentPlanId) {
        AssessmentScale scale = scaleMapper.findLatestByScaleCode(scaleCode);
        if (scale == null) throw new RuntimeException("量表不存在: " + scaleCode);

        return createAssignment(patientId, null, scale.getId(), scaleCode,
                assignedBy, isBaseline, "MANUAL", null, treatmentPlanId);
    }

    // ===== 任务管理 =====

    public List<AssessmentAssignment> getPatientPendingAssignments(Long patientId) {
        return assignmentMapper.findPendingByPatient(patientId);
    }

    public IPage<AssessmentAssignment> getPatientAssignmentHistory(Long patientId, int pageNum, int pageSize) {
        Page<AssessmentAssignment> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AssessmentAssignment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssessmentAssignment::getPatientId, patientId)
                .orderByDesc(AssessmentAssignment::getAssignedAt);
        return assignmentMapper.selectPage(page, wrapper);
    }

    /**
     * 患者开始做评估时，关联session到assignment
     */
    @Transactional
    public void linkSessionToAssignment(Long assignmentId, Long sessionId) {
        AssessmentAssignment assignment = assignmentMapper.selectById(assignmentId);
        if (assignment == null) return;
        assignment.setSessionId(sessionId);
        assignment.setAssignmentStatus("IN_PROGRESS");
        assignmentMapper.updateById(assignment);
    }

    /**
     * 评估完成后，更新assignment状态并计算基线变化
     */
    @Transactional
    public void onSessionCompleted(Long assignmentId, Long sessionId, Integer totalScore, String severityLevel) {
        AssessmentAssignment assignment = assignmentMapper.selectById(assignmentId);
        if (assignment == null) return;

        assignment.setAssignmentStatus("SUBMITTED");
        assignmentMapper.updateById(assignment);

        // 如果是基线评估，建立基线
        if ("Y".equals(assignment.getBaselineFlag())) {
            establishBaseline(assignment.getPatientId(), assignment.getScaleCode(),
                    sessionId, totalScore, severityLevel, assignment.getTreatmentPlanId());
        }

        // 计算相对基线变化
        AssessmentBaseline baseline = baselineMapper.findActiveBaseline(
                assignment.getPatientId(), assignment.getScaleCode());
        if (baseline != null && !baseline.getBaselineSessionId().equals(sessionId)) {
            BigDecimal change = BigDecimal.valueOf(totalScore - baseline.getTotalScore());
            // 更新session的变化值
            AssessmentSession session = sessionMapper.selectById(sessionId);
            if (session != null) {
                session.setChangeFromBaseline(change);
                // 判断临床显著性
                String significance = evaluateClinicalSignificance(
                        assignment.getScaleCode(), baseline.getTotalScore(), totalScore);
                session.setClinicalFlag(significance);
                sessionMapper.updateById(session);
            }
        }

        // 发布事件（供危机模块、治疗计划模块消费）
        publishAssessmentCompletedEvent(assignment, totalScore, severityLevel);
    }

    /**
     * 医生复核评估结果
     */
    @Transactional
    public void reviewAssignment(Long assignmentId, Long doctorId, String note) {
        AssessmentAssignment assignment = assignmentMapper.selectById(assignmentId);
        if (assignment == null || !"SUBMITTED".equals(assignment.getAssignmentStatus())) {
            throw new RuntimeException("评估任务不存在或不可复核");
        }
        assignment.setAssignmentStatus("REVIEWED");
        assignment.setReviewedBy(doctorId);
        assignment.setReviewedAt(LocalDateTime.now());
        assignment.setReviewNote(note);
        assignmentMapper.updateById(assignment);
    }

    // ===== 基线管理 =====

    @Transactional
    public void establishBaseline(Long patientId, String scaleCode, Long sessionId,
                                   Integer totalScore, String severityLevel, Long treatmentPlanId) {
        // 替代旧基线
        AssessmentBaseline oldBaseline = baselineMapper.findActiveBaseline(patientId, scaleCode);

        AssessmentBaseline baseline = new AssessmentBaseline();
        baseline.setPatientId(patientId);
        baseline.setScaleCode(scaleCode);
        baseline.setBaselineSessionId(sessionId);
        baseline.setTreatmentPlanId(treatmentPlanId);
        baseline.setTotalScore(totalScore);
        baseline.setSeverityLevel(severityLevel);
        baseline.setEstablishedAt(LocalDateTime.now());
        baselineMapper.insert(baseline);

        if (oldBaseline != null) {
            oldBaseline.setSupersededAt(LocalDateTime.now());
            oldBaseline.setSupersededBy(baseline.getId());
            baselineMapper.updateById(oldBaseline);
        }

        log.info("基线建立: 患者#{}, 量表={}, 分数={}", patientId, scaleCode, totalScore);
    }

    public AssessmentBaseline getActiveBaseline(Long patientId, String scaleCode) {
        return baselineMapper.findActiveBaseline(patientId, scaleCode);
    }

    // ===== 维度评分 =====

    @Transactional
    public void saveDimensionResults(Long sessionId, Long patientId, String scaleCode,
                                     List<AssessmentDimensionResult> results) {
        AssessmentBaseline baseline = baselineMapper.findActiveBaseline(patientId, scaleCode);

        for (AssessmentDimensionResult result : results) {
            result.setSessionId(sessionId);
            result.setPatientId(patientId);
            result.setScaleCode(scaleCode);

            // 计算相对基线变化
            if (baseline != null && baseline.getDimensionScoresJson() != null) {
                Map<String, Integer> baseScores = JSON.parseObject(
                        baseline.getDimensionScoresJson(), Map.class);
                Integer baseScore = baseScores.get(result.getDimensionCode());
                if (baseScore != null) {
                    result.setChangeFromBaseline(BigDecimal.valueOf(result.getRawScore() - baseScore));
                    result.setClinicalSignificance(evaluateDimensionSignificance(
                            result.getRawScore(), baseScore));
                }
            }

            dimensionResultMapper.insert(result);
        }
    }

    public List<AssessmentDimensionResult> getDimensionResults(Long sessionId) {
        LambdaQueryWrapper<AssessmentDimensionResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssessmentDimensionResult::getSessionId, sessionId)
                .orderByAsc(AssessmentDimensionResult::getDimensionCode);
        return dimensionResultMapper.selectList(wrapper);
    }

    /**
     * 获取患者某维度的趋势数据
     */
    public List<AssessmentDimensionResult> getDimensionTrend(Long patientId, String scaleCode,
                                                              String dimensionCode, int limit) {
        LambdaQueryWrapper<AssessmentDimensionResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssessmentDimensionResult::getPatientId, patientId)
                .eq(AssessmentDimensionResult::getScaleCode, scaleCode)
                .eq(AssessmentDimensionResult::getDimensionCode, dimensionCode)
                .orderByDesc(AssessmentDimensionResult::getCreateTime)
                .last("LIMIT " + limit);
        return dimensionResultMapper.selectList(wrapper);
    }

    // ===== 逾期检查调度器 =====

    @Scheduled(fixedDelay = 3600_000) // 每小时
    public void checkOverdueAssignments() {
        List<AssessmentAssignment> overdue = assignmentMapper.findOverdueAssignments();
        for (AssessmentAssignment a : overdue) {
            a.setAssignmentStatus("OVERDUE");
            assignmentMapper.updateById(a);

            // 发送逾期通知
            CreateNotificationCommand cmd = new CreateNotificationCommand();
            cmd.setUserId(a.getPatientId());
            cmd.setCategory("ASSESSMENT");
            cmd.setPriority("HIGH");
            cmd.setTitle("评估已逾期：" + a.getScaleCode());
            cmd.setContent("您的" + a.getScaleCode() + "评估已逾期，请尽快完成。");
            cmd.setSourceType("ASSESSMENT_ASSIGNMENT");
            cmd.setSourceId(a.getId());
            try {
                notificationService.createNotification(cmd);
            } catch (Exception e) {
                log.error("发送逾期通知失败: {}", e.getMessage());
            }

            log.info("评估任务#{} 已逾期, 患者#{}", a.getId(), a.getPatientId());
        }
    }

    // ===== 内部方法 =====

    private Long createAssignment(Long patientId, Long protocolId, Long scaleId, String scaleCode,
                                   Long assignedBy, boolean isBaseline,
                                   String sourceEventType, Long sourceEventId, Long treatmentPlanId) {
        AssessmentAssignment assignment = new AssessmentAssignment();
        assignment.setPatientId(patientId);
        assignment.setProtocolId(protocolId);
        assignment.setScaleId(scaleId);
        assignment.setScaleCode(scaleCode);
        assignment.setAssignedBy(assignedBy);
        assignment.setAssignedAt(LocalDateTime.now());
        assignment.setDueAt(LocalDateTime.now().plusDays(7)); // 默认7天截止
        assignment.setAssignmentStatus("ASSIGNED");
        assignment.setReminderSentCount(0);
        assignment.setBaselineFlag(isBaseline ? "Y" : "N");
        assignment.setTreatmentPlanId(treatmentPlanId);
        assignment.setSourceEventType(sourceEventType);
        assignment.setSourceEventId(sourceEventId);
        assignmentMapper.insert(assignment);

        // 发送通知
        CreateNotificationCommand cmd = new CreateNotificationCommand();
        cmd.setUserId(patientId);
        cmd.setCategory("ASSESSMENT");
        cmd.setPriority("NORMAL");
        cmd.setTitle("新评估任务：" + scaleCode);
        cmd.setContent("您有一份新的评估需要完成，截止时间：" + assignment.getDueAt().toLocalDate());
        cmd.setActionType("ROUTE");
        cmd.setSourceType("ASSESSMENT_ASSIGNMENT");
        cmd.setSourceId(assignment.getId());
        try {
            notificationService.createNotification(cmd);
        } catch (Exception e) {
            log.error("发送评估分配通知失败: {}", e.getMessage());
        }

        log.info("评估任务分配: 患者#{}, 量表={}, 基线={}", patientId, scaleCode, isBaseline);
        return assignment.getId();
    }

    private boolean hasPendingAssignment(Long patientId, String scaleCode) {
        LambdaQueryWrapper<AssessmentAssignment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssessmentAssignment::getPatientId, patientId)
                .eq(AssessmentAssignment::getScaleCode, scaleCode)
                .in(AssessmentAssignment::getAssignmentStatus, "ASSIGNED", "IN_PROGRESS");
        return assignmentMapper.selectCount(wrapper) > 0;
    }

    private String evaluateClinicalSignificance(String scaleCode, int baselineScore, int currentScore) {
        int change = currentScore - baselineScore;
        // 临床显著变化阈值 (simplified - would be scale-specific in production)
        int threshold = switch (scaleCode.toUpperCase()) {
            case "PHQ9" -> 5;  // PHQ-9 临床显著变化 = 5分
            case "GAD7" -> 4;  // GAD-7 临床显著变化 = 4分
            default -> 5;
        };

        if (change <= -threshold) return "IMPROVED";
        if (change >= threshold) return "DETERIORATED";
        if (Math.abs(change) >= 2) return change < 0 ? "IMPROVED" : "DETERIORATED";
        return "STABLE";
    }

    private String evaluateDimensionSignificance(int currentScore, int baselineScore) {
        int change = currentScore - baselineScore;
        if (change <= -2) return "IMPROVED";
        if (change >= 2) return "DETERIORATED";
        return "STABLE";
    }

    private void publishAssessmentCompletedEvent(AssessmentAssignment assignment,
                                                  Integer totalScore, String severityLevel) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("assignmentId", assignment.getId());
            payload.put("patientId", assignment.getPatientId());
            payload.put("scaleCode", assignment.getScaleCode());
            payload.put("totalScore", totalScore);
            payload.put("severityLevel", severityLevel);
            payload.put("isBaseline", "Y".equals(assignment.getBaselineFlag()));
            outboxService.append("ASSESSMENT_ASSIGNMENT", assignment.getId(),
                    "ASSESSMENT_ASSIGNMENT_COMPLETED",
                    "ASSESSMENT_ASSIGNMENT_COMPLETED:" + assignment.getId(),
                    JSON.toJSONString(payload));
        } catch (Exception e) {
            log.error("发布评估完成事件失败: {}", e.getMessage());
        }
    }
}
