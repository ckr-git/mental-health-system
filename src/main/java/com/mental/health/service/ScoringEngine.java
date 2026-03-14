package com.mental.health.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.mental.health.entity.AssessmentResponse;
import com.mental.health.entity.AssessmentScale;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ScoringEngine {

    public ScoreResult score(AssessmentScale scale, List<AssessmentResponse> responses) {
        int totalScore = responses.stream()
                .map(AssessmentResponse::getAnswerValue)
                .filter(v -> v != null)
                .mapToInt(Integer::intValue)
                .sum();

        Map<String, Object> breakdown = new LinkedHashMap<>();
        breakdown.put("method", "SUM");
        breakdown.put("totalScore", totalScore);
        breakdown.put("answerCount", responses.size());

        List<Map<String, Object>> answerDetails = new ArrayList<>();
        for (AssessmentResponse response : responses) {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("itemId", response.getItemId());
            detail.put("answerValue", response.getAnswerValue());
            detail.put("answerLabel", response.getAnswerLabel());
            answerDetails.add(detail);
        }
        breakdown.put("answers", answerDetails);

        SeverityResult severity = resolveSeverity(scale.getInterpretationRule(), totalScore);

        ScoreResult result = new ScoreResult();
        result.setTotalScore(totalScore);
        result.setSeverityLevel(severity.getLevel());
        result.setInterpretation(severity.getText());
        result.setScoreBreakdown(JSON.toJSONString(breakdown));
        return result;
    }

    private SeverityResult resolveSeverity(String interpretationRule, int totalScore) {
        JSONObject rule = JSON.parseObject(interpretationRule);
        if (rule != null) {
            JSONArray ranges = rule.getJSONArray("ranges");
            if (ranges != null) {
                for (int i = 0; i < ranges.size(); i++) {
                    JSONObject range = ranges.getJSONObject(i);
                    Integer min = range.getInteger("min");
                    Integer max = range.getInteger("max");
                    if (min != null && max != null && totalScore >= min && totalScore <= max) {
                        SeverityResult r = new SeverityResult();
                        r.setLevel(range.getString("label"));
                        r.setText(range.getString("text"));
                        return r;
                    }
                }
            }
        }
        SeverityResult fallback = new SeverityResult();
        fallback.setLevel("未知");
        fallback.setText("评测完成，当前严重程度为：未知");
        return fallback;
    }

    @Data
    public static class ScoreResult {
        private Integer totalScore;
        private String severityLevel;
        private String interpretation;
        private String scoreBreakdown;
    }

    @Data
    private static class SeverityResult {
        private String level;
        private String text;
    }
}
