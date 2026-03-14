package com.mental.health.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mental.health.entity.RiskRule;
import com.mental.health.mapper.MoodDiaryMapper;
import com.mental.health.mapper.RiskRuleMapper;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class RiskScoringService {

    @Autowired
    private RiskRuleMapper riskRuleMapper;

    @Autowired
    private MoodDiaryMapper moodDiaryMapper;

    public RiskEvaluation evaluate(String sourceType, Long userId, String textContent,
                                    Map<String, Object> structuredData) {
        List<RiskRule> rules = riskRuleMapper.findAllActive();
        int totalScore = 0;
        String maxLevel = "LOW";
        List<MatchedRule> matched = new ArrayList<>();

        for (RiskRule rule : rules) {
            boolean hit = switch (rule.getMatcherType()) {
                case "KEYWORD" -> keywordMatch(rule, sourceType, textContent);
                case "PHQ9_Q9" -> phq9Q9Match(rule, sourceType, structuredData);
                case "TREND_THRESHOLD" -> trendMatch(rule, userId);
                default -> false;
            };

            if (hit) {
                totalScore += rule.getScoreWeight();
                maxLevel = higherLevel(maxLevel, rule.getRiskLevel());
                matched.add(new MatchedRule(rule.getRuleCode(), rule.getRuleName(),
                        rule.getRiskLevel(), rule.getScoreWeight()));
            }
        }

        String finalLevel = normalizeByScore(totalScore);
        finalLevel = higherLevel(finalLevel, maxLevel);

        RiskEvaluation eval = new RiskEvaluation();
        eval.setTotalScore(totalScore);
        eval.setFinalLevel(finalLevel);
        eval.setMatchedRules(matched);
        return eval;
    }

    private boolean keywordMatch(RiskRule rule, String sourceType, String text) {
        if (text == null || text.isBlank()) return false;
        if (!isSourceMatch(rule.getSourceType(), sourceType)) return false;
        String[] keywords = rule.getKeywordPattern().split(",");
        for (String kw : keywords) {
            if (text.contains(kw.trim())) return true;
        }
        return false;
    }

    private boolean phq9Q9Match(RiskRule rule, String sourceType, Map<String, Object> data) {
        if (!"ASSESSMENT_COMPLETED".equals(sourceType)) return false;
        if (data == null) return false;
        Object answers = data.get("answers");
        if (answers instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> answersMap = (Map<String, Object>) answers;
            Object q9 = answersMap.get("PHQ9_9");
            if (q9 instanceof Number) {
                return ((Number) q9).intValue() >= 1;
            }
        }
        return false;
    }

    private boolean trendMatch(RiskRule rule, Long userId) {
        if (rule.getThresholdConfig() == null) return false;
        try {
            JSONObject config = JSON.parseObject(rule.getThresholdConfig());
            String field = config.getString("field");
            String op = config.getString("op");
            Double threshold = config.getDouble("value");
            if (field == null || op == null || threshold == null) return false;

            Double actual = switch (field) {
                case "moodAvg3d" -> moodDiaryMapper.avgMoodScore(userId, 3);
                case "moodAvg7d" -> moodDiaryMapper.avgMoodScore(userId, 7);
                case "sleepAvg3d" -> moodDiaryMapper.avgSleepQuality(userId, 3);
                default -> null;
            };

            if (actual == null) return false;
            return switch (op) {
                case "<=" -> actual <= threshold;
                case "<" -> actual < threshold;
                case ">=" -> actual >= threshold;
                case ">" -> actual > threshold;
                default -> false;
            };
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isSourceMatch(String ruleSource, String eventSource) {
        if ("TREND".equals(ruleSource)) return true;
        return ruleSource.equals(eventSource) ||
                (ruleSource.equals("DIARY") && "DIARY_CREATED".equals(eventSource)) ||
                (ruleSource.equals("CHAT") && "CHAT_MESSAGE_CREATED".equals(eventSource)) ||
                (ruleSource.equals("ASSESSMENT") && "ASSESSMENT_COMPLETED".equals(eventSource));
    }

    private String normalizeByScore(int score) {
        if (score >= 60) return "CRITICAL";
        if (score >= 40) return "HIGH";
        if (score >= 20) return "MEDIUM";
        return "LOW";
    }

    private String higherLevel(String a, String b) {
        return levelOrder(a) >= levelOrder(b) ? a : b;
    }

    private int levelOrder(String level) {
        return switch (level) {
            case "CRITICAL" -> 4;
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            default -> 1;
        };
    }

    @Data
    public static class RiskEvaluation {
        private int totalScore;
        private String finalLevel;
        private List<MatchedRule> matchedRules;
    }

    @Data
    public static class MatchedRule {
        private String ruleCode;
        private String ruleName;
        private String riskLevel;
        private int scoreWeight;

        public MatchedRule(String ruleCode, String ruleName, String riskLevel, int scoreWeight) {
            this.ruleCode = ruleCode;
            this.ruleName = ruleName;
            this.riskLevel = riskLevel;
            this.scoreWeight = scoreWeight;
        }
    }
}
