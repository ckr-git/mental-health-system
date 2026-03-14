package com.mental.health.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mental.health.entity.*;
import com.mental.health.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class TreatmentPlanService {

    @Autowired
    private TreatmentPlanMapper planMapper;

    @Autowired
    private TreatmentGoalMapper goalMapper;

    @Autowired
    private TreatmentInterventionMapper interventionMapper;

    @Autowired
    private SessionNoteMapper noteMapper;

    @Autowired
    private GoalProgressLogMapper progressLogMapper;

    @Autowired
    private OutboxService outboxService;

    // ===== Plan CRUD =====

    public List<TreatmentPlan> getDoctorPlans(Long doctorId, String status) {
        LambdaQueryWrapper<TreatmentPlan> w = new LambdaQueryWrapper<>();
        w.eq(TreatmentPlan::getDoctorId, doctorId);
        if (status != null) w.eq(TreatmentPlan::getPlanStatus, status);
        w.orderByDesc(TreatmentPlan::getUpdateTime);
        return planMapper.selectList(w);
    }

    public List<TreatmentPlan> getPatientPlans(Long patientId) {
        LambdaQueryWrapper<TreatmentPlan> w = new LambdaQueryWrapper<>();
        w.eq(TreatmentPlan::getPatientId, patientId)
         .in(TreatmentPlan::getPlanStatus, "ACTIVE", "PAUSED", "COMPLETED");
        w.orderByDesc(TreatmentPlan::getUpdateTime);
        return planMapper.selectList(w);
    }

    public Map<String, Object> getPlanDetail(Long planId) {
        TreatmentPlan plan = planMapper.selectById(planId);
        if (plan == null) return null;

        List<TreatmentGoal> goals = goalMapper.selectList(
                new LambdaQueryWrapper<TreatmentGoal>()
                        .eq(TreatmentGoal::getPlanId, planId)
                        .orderByAsc(TreatmentGoal::getSortOrder));

        List<TreatmentIntervention> interventions = interventionMapper.selectList(
                new LambdaQueryWrapper<TreatmentIntervention>()
                        .eq(TreatmentIntervention::getPlanId, planId)
                        .orderByAsc(TreatmentIntervention::getSortOrder));

        List<SessionNote> notes = noteMapper.selectList(
                new LambdaQueryWrapper<SessionNote>()
                        .eq(SessionNote::getPlanId, planId)
                        .orderByDesc(SessionNote::getSessionDate));

        Map<String, Object> detail = new HashMap<>();
        detail.put("plan", plan);
        detail.put("goals", goals);
        detail.put("interventions", interventions);
        detail.put("sessionNotes", notes);

        // Calculate progress
        int totalGoals = goals.size();
        int achievedGoals = (int) goals.stream().filter(g -> "ACHIEVED".equals(g.getStatus())).count();
        int avgProgress = goals.isEmpty() ? 0 :
                goals.stream().mapToInt(g -> g.getProgressPct() != null ? g.getProgressPct() : 0).sum() / totalGoals;
        detail.put("totalGoals", totalGoals);
        detail.put("achievedGoals", achievedGoals);
        detail.put("avgProgress", avgProgress);
        detail.put("sessionCount", notes.size());

        return detail;
    }

    @Transactional
    public TreatmentPlan createPlan(TreatmentPlan plan) {
        if (plan.getPlanStatus() == null) plan.setPlanStatus("DRAFT");
        if (plan.getPlanVersion() == null) plan.setPlanVersion(1);
        planMapper.insert(plan);
        return plan;
    }

    @Transactional
    public TreatmentPlan updatePlan(TreatmentPlan plan) {
        planMapper.updateById(plan);
        return plan;
    }

    @Transactional
    public boolean activatePlan(Long planId, Long doctorId) {
        TreatmentPlan plan = planMapper.selectById(planId);
        if (plan == null || !plan.getDoctorId().equals(doctorId)) return false;
        plan.setPlanStatus("ACTIVE");
        if (plan.getStartDate() == null) plan.setStartDate(LocalDate.now());
        planMapper.updateById(plan);

        String eventKey = "TREATMENT_PLAN_ACTIVATED:" + planId;
        String payload = String.format("{\"planId\":%d,\"patientId\":%d,\"doctorId\":%d,\"title\":\"%s\"}",
                planId, plan.getPatientId(), doctorId, plan.getTitle());
        outboxService.append("TREATMENT_PLAN", planId, "TREATMENT_PLAN_ACTIVATED", eventKey, payload);
        return true;
    }

    @Transactional
    public boolean completePlan(Long planId, Long doctorId) {
        TreatmentPlan plan = planMapper.selectById(planId);
        if (plan == null || !plan.getDoctorId().equals(doctorId)) return false;
        plan.setPlanStatus("COMPLETED");
        plan.setActualEndDate(LocalDate.now());
        planMapper.updateById(plan);
        return true;
    }

    // ===== Goals =====

    @Transactional
    public TreatmentGoal addGoal(TreatmentGoal goal) {
        if (goal.getStatus() == null) goal.setStatus("PENDING");
        if (goal.getProgressPct() == null) goal.setProgressPct(0);
        goalMapper.insert(goal);
        return goal;
    }

    @Transactional
    public TreatmentGoal updateGoal(TreatmentGoal goal) {
        goalMapper.updateById(goal);
        return goal;
    }

    @Transactional
    public boolean updateGoalProgress(Long goalId, Long loggedBy, int progressPct, String note) {
        TreatmentGoal goal = goalMapper.selectById(goalId);
        if (goal == null) return false;

        goal.setProgressPct(progressPct);
        if (progressPct >= 100) {
            goal.setStatus("ACHIEVED");
            goal.setAchievedAt(LocalDateTime.now());
        } else if (progressPct > 0 && "PENDING".equals(goal.getStatus())) {
            goal.setStatus("IN_PROGRESS");
        }
        goalMapper.updateById(goal);

        GoalProgressLog log = new GoalProgressLog();
        log.setGoalId(goalId);
        log.setLoggedBy(loggedBy);
        log.setProgressPct(progressPct);
        log.setNote(note);
        log.setLogDate(LocalDate.now());
        progressLogMapper.insert(log);

        return true;
    }

    public List<GoalProgressLog> getGoalProgressHistory(Long goalId) {
        return progressLogMapper.selectList(
                new LambdaQueryWrapper<GoalProgressLog>()
                        .eq(GoalProgressLog::getGoalId, goalId)
                        .orderByDesc(GoalProgressLog::getLogDate));
    }

    // ===== Interventions =====

    @Transactional
    public TreatmentIntervention addIntervention(TreatmentIntervention intervention) {
        if (intervention.getStatus() == null) intervention.setStatus("ACTIVE");
        interventionMapper.insert(intervention);
        return intervention;
    }

    @Transactional
    public TreatmentIntervention updateIntervention(TreatmentIntervention intervention) {
        interventionMapper.updateById(intervention);
        return intervention;
    }

    // ===== Session Notes =====

    @Transactional
    public SessionNote createSessionNote(SessionNote note) {
        noteMapper.insert(note);
        return note;
    }

    @Transactional
    public SessionNote updateSessionNote(SessionNote note) {
        noteMapper.updateById(note);
        return note;
    }

    public List<SessionNote> getPatientSessionNotes(Long patientId) {
        return noteMapper.selectList(
                new LambdaQueryWrapper<SessionNote>()
                        .eq(SessionNote::getPatientId, patientId)
                        .orderByDesc(SessionNote::getSessionDate));
    }

    public SessionNote getSessionNote(Long noteId) {
        return noteMapper.selectById(noteId);
    }
}
