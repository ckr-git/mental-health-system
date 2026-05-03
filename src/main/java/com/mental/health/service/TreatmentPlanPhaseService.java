package com.mental.health.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mental.health.entity.*;
import com.mental.health.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 治疗计划阶段化服务
 *
 * 计划状态机: DRAFT → ACTIVE → PAUSED → COMPLETED / ARCHIVED
 * 阶段状态机: PLANNED → ACTIVE → COMPLETED / CANCELLED / SKIPPED
 * 评审状态机: PENDING → IN_PROGRESS → COMPLETED / REOPENED
 * 修订状态机: PROPOSED → APPROVED → APPLIED / REJECTED
 * 任务状态机: ASSIGNED → IN_PROGRESS → SUBMITTED → REVIEWED → COMPLETED / OVERDUE / CANCELLED
 */
@Service
public class TreatmentPlanPhaseService {

    private static final Logger log = LoggerFactory.getLogger(TreatmentPlanPhaseService.class);

    @Autowired private TreatmentPlanMapper planMapper;
    @Autowired private TreatmentPlanPhaseMapper phaseMapper;
    @Autowired private TreatmentPlanReviewMapper reviewMapper;
    @Autowired private TreatmentPlanRevisionMapper revisionMapper;
    @Autowired private InterventionTaskMapper taskMapper;
    @Autowired private TreatmentGoalMapper goalMapper;
    @Autowired private OutboxService outboxService;

    // ===== 阶段管理 =====

    /**
     * 为计划创建默认阶段
     */
    @Transactional
    public void initDefaultPhases(Long planId) {
        String[][] defaults = {
                {"INTAKE", "评估接诊阶段", "完成首诊评估和基线建立"},
                {"STABILIZATION", "稳定阶段", "建立治疗联盟，缓解急性症状"},
                {"ACTIVE_WORK", "核心治疗阶段", "实施主要干预策略"},
                {"MAINTENANCE", "维持阶段", "巩固治疗成果，预防复发"},
                {"DISCHARGE", "结束阶段", "总结评估，制定预防计划"}
        };

        for (int i = 0; i < defaults.length; i++) {
            TreatmentPlanPhase phase = new TreatmentPlanPhase();
            phase.setPlanId(planId);
            phase.setPhaseCode(defaults[i][0]);
            phase.setPhaseName(defaults[i][1]);
            phase.setPhaseOrder(i + 1);
            phase.setPhaseStatus("PLANNED");
            phase.setExitCriteria(defaults[i][2]);
            phaseMapper.insert(phase);
        }
    }

    @Transactional
    public void activatePhase(Long planId, Long phaseId, Long operatorId) {
        TreatmentPlanPhase phase = phaseMapper.selectById(phaseId);
        if (phase == null || !phase.getPlanId().equals(planId)) throw new RuntimeException("阶段不存在");
        if (!"PLANNED".equals(phase.getPhaseStatus())) throw new RuntimeException("只能激活PLANNED状态的阶段");

        phase.setPhaseStatus("ACTIVE");
        phase.setActualStartDate(LocalDate.now());
        phase.setActivatedBy(operatorId);
        phaseMapper.updateById(phase);

        // 更新计划当前阶段
        TreatmentPlan plan = planMapper.selectById(planId);
        if (plan != null) {
            plan.setCurrentPhaseId(phaseId);
            if ("DRAFT".equals(plan.getPlanStatus())) {
                plan.setPlanStatus("ACTIVE");
                plan.setStartDate(LocalDate.now());
            }
            // 设置下次评审时间
            plan.setNextReviewAt(LocalDateTime.now().plusDays(plan.getReviewIntervalDays() != null ? plan.getReviewIntervalDays() : 14));
            planMapper.updateById(plan);
        }

        // 自动创建评审节点
        createScheduledReview(planId, phaseId, LocalDateTime.now().plusDays(14));

        log.info("阶段#{} 已激活, 计划#{}", phaseId, planId);
    }

    @Transactional
    public void completePhase(Long planId, Long phaseId, Long operatorId, String note) {
        TreatmentPlanPhase phase = phaseMapper.selectById(phaseId);
        if (phase == null || !"ACTIVE".equals(phase.getPhaseStatus())) throw new RuntimeException("阶段不可完成");

        phase.setPhaseStatus("COMPLETED");
        phase.setActualEndDate(LocalDate.now());
        phase.setCompletedBy(operatorId);
        phase.setCompletionNote(note);
        phaseMapper.updateById(phase);

        // 尝试自动进入下一阶段
        TreatmentPlanPhase nextPhase = findNextPhase(planId, phase.getPhaseOrder());
        if (nextPhase != null) {
            activatePhase(planId, nextPhase.getId(), operatorId);
        } else {
            // 所有阶段完成 → 计划完成
            TreatmentPlan plan = planMapper.selectById(planId);
            if (plan != null) {
                plan.setPlanStatus("COMPLETED");
                plan.setActualEndDate(LocalDate.now());
                planMapper.updateById(plan);
            }
        }

        log.info("阶段#{} 已完成, 计划#{}", phaseId, planId);
    }

    public List<TreatmentPlanPhase> getPlanPhases(Long planId) {
        LambdaQueryWrapper<TreatmentPlanPhase> w = new LambdaQueryWrapper<>();
        w.eq(TreatmentPlanPhase::getPlanId, planId)
                .orderByAsc(TreatmentPlanPhase::getPhaseOrder);
        return phaseMapper.selectList(w);
    }

    // ===== 评审 =====

    @Transactional
    public Long createScheduledReview(Long planId, Long phaseId, LocalDateTime dueAt) {
        TreatmentPlanReview review = new TreatmentPlanReview();
        review.setPlanId(planId);
        review.setPhaseId(phaseId);
        review.setReviewType("SCHEDULED");
        review.setReviewStatus("PENDING");
        review.setDueAt(dueAt);
        reviewMapper.insert(review);
        return review.getId();
    }

    @Transactional
    public void completeReview(Long reviewId, Long reviewerId, String conclusionCode,
                                String summaryText, String actionPlanJson) {
        TreatmentPlanReview review = reviewMapper.selectById(reviewId);
        if (review == null) throw new RuntimeException("评审不存在");

        review.setReviewStatus("COMPLETED");
        review.setReviewerId(reviewerId);
        review.setCompletedAt(LocalDateTime.now());
        review.setConclusionCode(conclusionCode);
        review.setSummaryText(summaryText);
        review.setActionPlanJson(actionPlanJson);
        reviewMapper.updateById(review);

        // 根据结论触发后续动作
        TreatmentPlan plan = planMapper.selectById(review.getPlanId());
        if (plan != null) {
            switch (conclusionCode) {
                case "CONTINUE" -> {
                    // 创建下次评审
                    int interval = plan.getReviewIntervalDays() != null ? plan.getReviewIntervalDays() : 14;
                    createScheduledReview(plan.getId(), review.getPhaseId(),
                            LocalDateTime.now().plusDays(interval));
                    plan.setNextReviewAt(LocalDateTime.now().plusDays(interval));
                    planMapper.updateById(plan);
                }
                case "ADJUST" -> {
                    // 标记需要修订
                    log.info("评审结论为ADJUST, 等待医生提交修订, 计划#{}", plan.getId());
                }
                case "ESCALATE" -> {
                    plan.setPlanStatus("PAUSED");
                    planMapper.updateById(plan);
                }
                case "REFER" -> {
                    log.info("评审结论为REFER, 需要发起转诊, 计划#{}", plan.getId());
                }
                case "DISCHARGE" -> {
                    if (review.getPhaseId() != null) {
                        completePhase(plan.getId(), review.getPhaseId(), reviewerId, "评审结论: 出院");
                    }
                }
            }
        }

        log.info("评审#{} 已完成, 结论={}", reviewId, conclusionCode);
    }

    public List<TreatmentPlanReview> getPlanReviews(Long planId, String status) {
        LambdaQueryWrapper<TreatmentPlanReview> w = new LambdaQueryWrapper<>();
        w.eq(TreatmentPlanReview::getPlanId, planId);
        if (status != null) w.eq(TreatmentPlanReview::getReviewStatus, status);
        w.orderByDesc(TreatmentPlanReview::getDueAt);
        return reviewMapper.selectList(w);
    }

    // ===== 修订 =====

    @Transactional
    public Long proposeRevision(Long planId, Long proposerId, String changeSummary,
                                 String changeDetailJson, String reason, Long reviewId) {
        TreatmentPlan plan = planMapper.selectById(planId);
        if (plan == null) throw new RuntimeException("计划不存在");

        TreatmentPlanRevision revision = new TreatmentPlanRevision();
        revision.setPlanId(planId);
        revision.setFromVersion(plan.getPlanVersion());
        revision.setToVersion(plan.getPlanVersion() + 1);
        revision.setRevisionStatus("PROPOSED");
        revision.setProposedBy(proposerId);
        revision.setProposedAt(LocalDateTime.now());
        revision.setChangeSummary(changeSummary);
        revision.setChangeDetailJson(changeDetailJson);
        revision.setRevisionReason(reason);
        revision.setReviewId(reviewId);
        revisionMapper.insert(revision);
        return revision.getId();
    }

    @Transactional
    public void approveAndApplyRevision(Long revisionId, Long approverId) {
        TreatmentPlanRevision revision = revisionMapper.selectById(revisionId);
        if (revision == null || !"PROPOSED".equals(revision.getRevisionStatus()))
            throw new RuntimeException("修订不存在或不可批准");

        revision.setRevisionStatus("APPLIED");
        revision.setApprovedBy(approverId);
        revision.setApprovedAt(LocalDateTime.now());
        revision.setAppliedAt(LocalDateTime.now());
        revisionMapper.updateById(revision);

        // 更新计划版本号
        TreatmentPlan plan = planMapper.selectById(revision.getPlanId());
        if (plan != null) {
            plan.setPlanVersion(revision.getToVersion());
            planMapper.updateById(plan);
        }

        log.info("修订#{} 已批准并生效, 计划#{} v{}", revisionId, revision.getPlanId(), revision.getToVersion());
    }

    // ===== 干预任务 =====

    @Transactional
    public Long assignTask(Long planId, Long phaseId, Long interventionId, Long patientId,
                            Long assignedBy, String taskType, String title, String description,
                            LocalDateTime dueAt) {
        InterventionTask task = new InterventionTask();
        task.setPlanId(planId);
        task.setPhaseId(phaseId);
        task.setInterventionId(interventionId);
        task.setPatientId(patientId);
        task.setAssignedBy(assignedBy);
        task.setTaskType(taskType);
        task.setTitle(title);
        task.setDescription(description);
        task.setTaskStatus("ASSIGNED");
        task.setAssignedAt(LocalDateTime.now());
        task.setDueAt(dueAt != null ? dueAt : LocalDateTime.now().plusDays(7));
        taskMapper.insert(task);
        return task.getId();
    }

    @Transactional
    public void submitTask(Long taskId, Long patientId, String note, String value) {
        InterventionTask task = taskMapper.selectById(taskId);
        if (task == null || !task.getPatientId().equals(patientId))
            throw new RuntimeException("任务不存在");
        if (!Set.of("ASSIGNED", "IN_PROGRESS").contains(task.getTaskStatus()))
            throw new RuntimeException("任务不可提交");

        task.setTaskStatus("SUBMITTED");
        task.setSubmittedAt(LocalDateTime.now());
        task.setSubmissionNote(note);
        task.setSubmissionValue(value);
        taskMapper.updateById(task);
    }

    @Transactional
    public void reviewTask(Long taskId, Long doctorId, String resultCode, String note) {
        InterventionTask task = taskMapper.selectById(taskId);
        if (task == null || !"SUBMITTED".equals(task.getTaskStatus()))
            throw new RuntimeException("任务不存在或不可复核");

        task.setTaskStatus("REDO".equals(resultCode) ? "ASSIGNED" : "COMPLETED");
        task.setReviewedBy(doctorId);
        task.setReviewedAt(LocalDateTime.now());
        task.setReviewResultCode(resultCode);
        task.setReviewNote(note);
        taskMapper.updateById(task);

        // 更新依从性
        updateAdherence(task.getPlanId());
    }

    public List<InterventionTask> getPatientTasks(Long patientId, String status) {
        LambdaQueryWrapper<InterventionTask> w = new LambdaQueryWrapper<>();
        w.eq(InterventionTask::getPatientId, patientId);
        if (status != null) w.eq(InterventionTask::getTaskStatus, status);
        w.orderByAsc(InterventionTask::getDueAt);
        return taskMapper.selectList(w);
    }

    public List<InterventionTask> getPlanTasks(Long planId) {
        LambdaQueryWrapper<InterventionTask> w = new LambdaQueryWrapper<>();
        w.eq(InterventionTask::getPlanId, planId)
                .orderByAsc(InterventionTask::getDueAt);
        return taskMapper.selectList(w);
    }

    // ===== 依从性 =====

    private void updateAdherence(Long planId) {
        LambdaQueryWrapper<InterventionTask> w = new LambdaQueryWrapper<>();
        w.eq(InterventionTask::getPlanId, planId)
                .ne(InterventionTask::getTaskStatus, "CANCELLED");
        List<InterventionTask> allTasks = taskMapper.selectList(w);
        if (allTasks.isEmpty()) return;

        long completed = allTasks.stream().filter(t -> "COMPLETED".equals(t.getTaskStatus())).count();
        double score = (double) completed / allTasks.size() * 100;

        TreatmentPlan plan = planMapper.selectById(planId);
        if (plan != null) {
            plan.setAdherenceScore(java.math.BigDecimal.valueOf(score));
            planMapper.updateById(plan);
        }
    }

    private TreatmentPlanPhase findNextPhase(Long planId, int currentOrder) {
        LambdaQueryWrapper<TreatmentPlanPhase> w = new LambdaQueryWrapper<>();
        w.eq(TreatmentPlanPhase::getPlanId, planId)
                .eq(TreatmentPlanPhase::getPhaseStatus, "PLANNED")
                .gt(TreatmentPlanPhase::getPhaseOrder, currentOrder)
                .orderByAsc(TreatmentPlanPhase::getPhaseOrder)
                .last("LIMIT 1");
        return phaseMapper.selectOne(w);
    }
}
