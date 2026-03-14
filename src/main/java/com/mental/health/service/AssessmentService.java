package com.mental.health.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mental.health.dto.*;
import com.mental.health.entity.*;
import com.mental.health.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AssessmentService {

    @Autowired
    private AssessmentScaleMapper assessmentScaleMapper;

    @Autowired
    private AssessmentScaleItemMapper assessmentScaleItemMapper;

    @Autowired
    private AssessmentSessionMapper assessmentSessionMapper;

    @Autowired
    private AssessmentResponseMapper assessmentResponseMapper;

    @Autowired
    private AssessmentReportMapper assessmentReportMapper;

    @Autowired
    private ScoringEngine scoringEngine;

    @Autowired
    private OutboxService outboxService;

    public List<ScaleListItem> getAvailableScales() {
        List<AssessmentScale> scales = assessmentScaleMapper.findActiveScales();
        Map<String, AssessmentScale> latestByCode = new LinkedHashMap<>();
        for (AssessmentScale scale : scales) {
            latestByCode.putIfAbsent(scale.getScaleCode(), scale);
        }

        return latestByCode.values().stream().map(scale -> {
            ScaleListItem item = new ScaleListItem();
            item.setId(scale.getId());
            item.setScaleCode(scale.getScaleCode());
            item.setScaleName(scale.getScaleName());
            item.setScaleType(scale.getScaleType());
            item.setIntroText(scale.getIntroText());
            item.setEstimatedMinutes(scale.getEstimatedMinutes());
            LambdaQueryWrapper<AssessmentScaleItem> w = new LambdaQueryWrapper<>();
            w.eq(AssessmentScaleItem::getScaleId, scale.getId());
            item.setItemCount(assessmentScaleItemMapper.selectCount(w));
            return item;
        }).collect(Collectors.toList());
    }

    public ScaleDetailDTO getScaleDetail(String scaleCode) {
        AssessmentScale scale = assessmentScaleMapper.findLatestByScaleCode(scaleCode);
        if (scale == null) {
            return null;
        }
        List<AssessmentScaleItem> items = assessmentScaleItemMapper.findByScaleId(scale.getId());

        ScaleDetailDTO dto = new ScaleDetailDTO();
        dto.setId(scale.getId());
        dto.setScaleCode(scale.getScaleCode());
        dto.setScaleName(scale.getScaleName());
        dto.setIntroText(scale.getIntroText());
        dto.setItems(items);
        return dto;
    }

    @Transactional
    public Long startSession(Long userId, String scaleCode) {
        AssessmentScale scale = assessmentScaleMapper.findLatestByScaleCode(scaleCode);
        if (scale == null) {
            throw new RuntimeException("评估量表不存在");
        }

        AssessmentSession session = new AssessmentSession();
        session.setUserId(userId);
        session.setScaleId(scale.getId());
        session.setSource("SELF");
        session.setSessionStatus("IN_PROGRESS");
        session.setStartedAt(LocalDateTime.now());
        assessmentSessionMapper.insert(session);
        return session.getId();
    }

    @Transactional
    public void saveAnswers(Long userId, Long sessionId, List<SaveAnswersRequest.AnswerItem> answers) {
        AssessmentSession session = lockOwnedInProgressSession(userId, sessionId);
        if (answers == null || answers.isEmpty()) {
            return;
        }

        for (SaveAnswersRequest.AnswerItem answer : answers) {
            AssessmentScaleItem item = assessmentScaleItemMapper.selectById(answer.getItemId());
            if (item == null || !session.getScaleId().equals(item.getScaleId())) {
                throw new RuntimeException("题目不存在或不属于当前量表");
            }

            validateAnswerValue(item, answer.getAnswerValue());
            assessmentResponseMapper.upsertAnswer(
                    sessionId, answer.getItemId(),
                    answer.getAnswerValue(), answer.getAnswerLabel());
        }
    }

    @Transactional
    public SessionResultDTO submitSession(Long userId, Long sessionId) {
        AssessmentSession session = lockOwnedInProgressSession(userId, sessionId);
        AssessmentScale scale = assessmentScaleMapper.selectById(session.getScaleId());
        if (scale == null) {
            throw new RuntimeException("评估量表不存在");
        }

        List<AssessmentScaleItem> items = assessmentScaleItemMapper.findByScaleId(scale.getId());
        List<AssessmentResponse> responses = assessmentResponseMapper.findBySessionId(sessionId);
        validateAllRequiredAnswered(items, responses);

        ScoringEngine.ScoreResult scoreResult = scoringEngine.score(scale, responses);

        LocalDateTime now = LocalDateTime.now();
        session.setSessionStatus("COMPLETED");
        session.setSubmittedAt(now);
        session.setScoredAt(now);
        session.setTotalScore(scoreResult.getTotalScore());
        session.setSeverityLevel(scoreResult.getSeverityLevel());
        session.setScoreBreakdown(scoreResult.getScoreBreakdown());
        session.setInterpretation(scoreResult.getInterpretation());
        assessmentSessionMapper.updateById(session);

        AssessmentReport report = buildReport(userId, scale, session, scoreResult);
        assessmentReportMapper.insert(report);

        session.setReportId(report.getId());
        assessmentSessionMapper.updateById(session);

        try {
            Map<String, Object> eventPayload = new HashMap<>();
            eventPayload.put("sessionId", session.getId());
            eventPayload.put("userId", userId);
            eventPayload.put("scaleCode", scale.getScaleCode());
            eventPayload.put("totalScore", scoreResult.getTotalScore());
            eventPayload.put("severityLevel", scoreResult.getSeverityLevel());
            Map<String, Integer> answersByCode = new HashMap<>();
            for (AssessmentResponse r : responses) {
                AssessmentScaleItem item = assessmentScaleItemMapper.selectById(r.getItemId());
                if (item != null) {
                    answersByCode.put(item.getItemCode(), r.getAnswerValue());
                }
            }
            eventPayload.put("answers", answersByCode);
            outboxService.append("ASSESSMENT_SESSION", session.getId(), "ASSESSMENT_COMPLETED",
                    "ASSESSMENT_COMPLETED:" + session.getId(), JSON.toJSONString(eventPayload));
        } catch (Exception e) {
            // outbox failure should not block assessment submission
        }

        SessionResultDTO result = new SessionResultDTO();
        result.setSessionId(session.getId());
        result.setScaleCode(scale.getScaleCode());
        result.setScaleName(scale.getScaleName());
        result.setTotalScore(scoreResult.getTotalScore());
        result.setSeverityLevel(scoreResult.getSeverityLevel());
        result.setInterpretation(scoreResult.getInterpretation());
        result.setScoreBreakdown(scoreResult.getScoreBreakdown());
        result.setSubmittedAt(session.getSubmittedAt());
        return result;
    }

    public IPage<AssessmentHistoryItem> getHistory(Long userId, int pageNum, int pageSize) {
        return buildHistoryPage(userId, pageNum, pageSize);
    }

    public IPage<AssessmentHistoryItem> getPatientAssessments(Long patientId, int pageNum, int pageSize) {
        return buildHistoryPage(patientId, pageNum, pageSize);
    }

    private IPage<AssessmentHistoryItem> buildHistoryPage(Long userId, int pageNum, int pageSize) {
        Page<AssessmentSession> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AssessmentSession> w = new LambdaQueryWrapper<>();
        w.eq(AssessmentSession::getUserId, userId)
                .eq(AssessmentSession::getSessionStatus, "COMPLETED")
                .orderByDesc(AssessmentSession::getSubmittedAt);

        IPage<AssessmentSession> sessionPage = assessmentSessionMapper.selectPage(page, w);

        Set<Long> scaleIds = sessionPage.getRecords().stream()
                .map(AssessmentSession::getScaleId)
                .collect(Collectors.toSet());
        Map<Long, AssessmentScale> scaleMap = new HashMap<>();
        for (Long scaleId : scaleIds) {
            AssessmentScale scale = assessmentScaleMapper.selectById(scaleId);
            if (scale != null) {
                scaleMap.put(scaleId, scale);
            }
        }

        Page<AssessmentHistoryItem> resultPage = new Page<>(pageNum, pageSize);
        resultPage.setTotal(sessionPage.getTotal());
        resultPage.setRecords(sessionPage.getRecords().stream().map(s -> {
            AssessmentScale scale = scaleMap.get(s.getScaleId());
            AssessmentHistoryItem item = new AssessmentHistoryItem();
            item.setSessionId(s.getId());
            item.setScaleCode(scale != null ? scale.getScaleCode() : null);
            item.setScaleName(scale != null ? scale.getScaleName() : null);
            item.setTotalScore(s.getTotalScore());
            item.setSeverityLevel(s.getSeverityLevel());
            item.setSubmittedAt(s.getSubmittedAt());
            return item;
        }).collect(Collectors.toList()));
        return resultPage;
    }

    private AssessmentSession lockOwnedInProgressSession(Long userId, Long sessionId) {
        AssessmentSession session = assessmentSessionMapper.lockOwnedInProgressSession(userId, sessionId);
        if (session == null) {
            throw new RuntimeException("评估会话不存在或不可编辑");
        }
        return session;
    }

    private void validateAnswerValue(AssessmentScaleItem item, Integer answerValue) {
        if (answerValue == null) {
            throw new RuntimeException("答案不能为空");
        }
        JSONArray options = JSON.parseArray(item.getAnswerOptions());
        if (options == null) {
            return;
        }
        for (int i = 0; i < options.size(); i++) {
            JSONObject option = options.getJSONObject(i);
            if (Objects.equals(option.getInteger("value"), answerValue)) {
                return;
            }
        }
        throw new RuntimeException("答案选项无效");
    }

    private void validateAllRequiredAnswered(List<AssessmentScaleItem> items, List<AssessmentResponse> responses) {
        Set<Long> answeredIds = responses.stream()
                .map(AssessmentResponse::getItemId)
                .collect(Collectors.toSet());
        for (AssessmentScaleItem item : items) {
            if (item.getRequiredFlag() != null && item.getRequiredFlag() == 1
                    && !answeredIds.contains(item.getId())) {
                throw new RuntimeException("请完成所有必答题后再提交");
            }
        }
    }

    private AssessmentReport buildReport(Long userId, AssessmentScale scale,
                                         AssessmentSession session, ScoringEngine.ScoreResult scoreResult) {
        AssessmentReport report = new AssessmentReport();
        report.setUserId(userId);
        report.setReportType("ASSESSMENT");
        report.setTitle(scale.getScaleName() + "评估报告");
        report.setContent(scoreResult.getInterpretation());
        report.setDiagnosis(scoreResult.getSeverityLevel());
        report.setSuggestions(scale.getScaleCode() + "评估结果为：" + scoreResult.getSeverityLevel()
                + "，建议按时复测并在必要时寻求专业帮助");
        report.setOverallScore(scoreResult.getTotalScore());
        report.setSummary(scoreResult.getInterpretation());
        report.setIsAiGenerated(0);
        report.setStatus(1);
        report.setAssessmentSessionId(session.getId());
        report.setScaleCode(scale.getScaleCode());
        report.setSeverityLevel(scoreResult.getSeverityLevel());
        report.setRawScoreJson(scoreResult.getScoreBreakdown());

        if ("PHQ9".equalsIgnoreCase(scale.getScaleCode())) {
            report.setDepressionScore(scoreResult.getTotalScore());
        } else if ("GAD7".equalsIgnoreCase(scale.getScaleCode())) {
            report.setAnxietyScore(scoreResult.getTotalScore());
        }
        return report;
    }
}
