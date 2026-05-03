package com.mental.health.service;

import com.alibaba.fastjson2.JSON;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 心情日记洞察服务 — 情绪/触发/应对标签抽取 + 模式汇总
 *
 * 处理流程:
 * 1. 日记创建后异步抽取特征（情绪标签、触发因素、应对方式、情感分数）
 * 2. 结果存入mood_diary_feature表
 * 3. 同步更新mood_diary表的快照字段
 * 4. 为医生生成近期情绪模式汇总
 */
@Service
public class MoodInsightService {

    private static final Logger log = LoggerFactory.getLogger(MoodInsightService.class);

    // 情绪词典
    private static final Map<String, String[]> EMOTION_DICT = Map.of(
            "JOY", new String[]{"开心", "快乐", "高兴", "愉快", "幸福", "满足", "欣慰", "兴奋"},
            "SADNESS", new String[]{"悲伤", "难过", "伤心", "沮丧", "低落", "失落", "心痛", "哭"},
            "ANXIETY", new String[]{"焦虑", "紧张", "担心", "不安", "恐惧", "害怕", "慌", "忐忑"},
            "ANGER", new String[]{"愤怒", "生气", "烦躁", "恼火", "暴怒", "不满", "委屈"},
            "CALM", new String[]{"平静", "安宁", "放松", "淡定", "祥和", "踏实", "舒适"},
            "FATIGUE", new String[]{"疲惫", "累", "精疲力竭", "无力", "困倦", "耗尽", "没精神"},
            "LONELY", new String[]{"孤独", "寂寞", "独处", "没人理解", "被孤立", "一个人"},
            "HOPEFUL", new String[]{"希望", "期待", "向往", "好转", "进步", "改善", "信心"}
    );

    // 触发因素词典
    private static final Map<String, String[]> TRIGGER_DICT = Map.of(
            "WORK", new String[]{"工作", "加班", "领导", "同事", "KPI", "项目", "deadline", "职场"},
            "RELATIONSHIP", new String[]{"伴侣", "恋人", "分手", "吵架", "感情", "婚姻", "男友", "女友"},
            "FAMILY", new String[]{"父母", "家人", "孩子", "家庭", "亲戚", "爸妈"},
            "HEALTH", new String[]{"身体", "疼痛", "失眠", "头痛", "生病", "不舒服"},
            "FINANCE", new String[]{"钱", "经济", "债务", "贷款", "工资", "房贷"},
            "STUDY", new String[]{"考试", "学习", "论文", "成绩", "毕业", "学校"},
            "SOCIAL", new String[]{"社交", "朋友", "聚会", "被排斥", "社恐", "人际"}
    );

    // 应对方式词典
    private static final Map<String, String[]> COPING_DICT = Map.of(
            "EXERCISE", new String[]{"运动", "跑步", "散步", "健身", "瑜伽", "游泳"},
            "MEDITATION", new String[]{"冥想", "深呼吸", "放松", "正念", "打坐"},
            "SOCIAL_SUPPORT", new String[]{"倾诉", "聊天", "陪伴", "安慰", "鼓励", "朋友帮助"},
            "CREATIVE", new String[]{"画画", "写作", "音乐", "唱歌", "弹琴", "读书"},
            "REST", new String[]{"休息", "睡觉", "躺平", "放假", "什么都不做"}
    );

    @Autowired private MoodDiaryFeatureMapper featureMapper;
    @Autowired private MoodDiaryMapper moodDiaryMapper;

    // ===== 特征抽取 =====

    @Transactional
    public void extractDiaryFeatures(Long diaryId) {
        MoodDiary diary = moodDiaryMapper.selectById(diaryId);
        if (diary == null || diary.getContent() == null || diary.getContent().isEmpty()) return;

        String content = diary.getContent();

        // 抽取情绪标签
        Map<String, Integer> emotionCounts = matchDictionary(content, EMOTION_DICT);
        String primaryEmotion = emotionCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse(null);

        // 抽取触发因素
        Map<String, Integer> triggerCounts = matchDictionary(content, TRIGGER_DICT);

        // 抽取应对方式
        Map<String, Integer> copingCounts = matchDictionary(content, COPING_DICT);

        // 计算情感分数 (-1到1)
        BigDecimal sentimentScore = calculateSentiment(emotionCounts, diary.getMoodScore());

        // 检测风险信号
        List<String> riskSignals = detectRiskSignals(content);

        // 生成AI摘要（简化版：基于抽取结果生成）
        String summary = generateInsightSummary(primaryEmotion, triggerCounts.keySet(), copingCounts.keySet(), diary.getMoodScore());

        // 保存特征
        MoodDiaryFeature feature = new MoodDiaryFeature();
        feature.setMoodDiaryId(diaryId);
        feature.setUserId(diary.getUserId());
        feature.setExtractionStatus("SUCCESS");
        feature.setSentimentScore(sentimentScore);
        feature.setPrimaryEmotionCode(primaryEmotion);
        feature.setEmotionTagsJson(JSON.toJSONString(emotionCounts.keySet()));
        feature.setTriggerTagsJson(JSON.toJSONString(triggerCounts.keySet()));
        feature.setCopingTagsJson(JSON.toJSONString(copingCounts.keySet()));
        feature.setRiskSignalJson(riskSignals.isEmpty() ? null : JSON.toJSONString(riskSignals));
        feature.setAiSummary(summary);
        feature.setExtractedAt(LocalDateTime.now());

        // Upsert
        LambdaQueryWrapper<MoodDiaryFeature> existW = new LambdaQueryWrapper<>();
        existW.eq(MoodDiaryFeature::getMoodDiaryId, diaryId);
        MoodDiaryFeature existing = featureMapper.selectOne(existW);
        if (existing != null) {
            feature.setId(existing.getId());
            featureMapper.updateById(feature);
        } else {
            featureMapper.insert(feature);
        }

        // 更新diary快照字段
        diary.setSentimentScore(sentimentScore);
        diary.setFeatureStatus("SUCCESS");
        diary.setEmotionTagsJson(feature.getEmotionTagsJson());
        diary.setTriggerTagsJson(feature.getTriggerTagsJson());
        diary.setCopingTagsJson(feature.getCopingTagsJson());
        diary.setAiSummary(summary);
        moodDiaryMapper.updateById(diary);
    }

    // ===== 洞察查询 =====

    public MoodDiaryFeature getDiaryInsight(Long diaryId) {
        LambdaQueryWrapper<MoodDiaryFeature> w = new LambdaQueryWrapper<>();
        w.eq(MoodDiaryFeature::getMoodDiaryId, diaryId);
        return featureMapper.selectOne(w);
    }

    /**
     * 汇总患者近期情绪模式
     */
    public Map<String, Object> summarizePatientMoodPattern(Long patientId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);

        LambdaQueryWrapper<MoodDiaryFeature> w = new LambdaQueryWrapper<>();
        w.eq(MoodDiaryFeature::getUserId, patientId)
                .eq(MoodDiaryFeature::getExtractionStatus, "SUCCESS")
                .ge(MoodDiaryFeature::getExtractedAt, since)
                .orderByDesc(MoodDiaryFeature::getExtractedAt);
        List<MoodDiaryFeature> features = featureMapper.selectList(w);

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalDiaries", features.size());
        summary.put("period", days + "天");

        // 情绪分布
        Map<String, Integer> emotionFreq = new HashMap<>();
        Map<String, Integer> triggerFreq = new HashMap<>();
        Map<String, Integer> copingFreq = new HashMap<>();
        double totalSentiment = 0;
        int sentimentCount = 0;

        for (MoodDiaryFeature f : features) {
            if (f.getPrimaryEmotionCode() != null) {
                emotionFreq.merge(f.getPrimaryEmotionCode(), 1, Integer::sum);
            }
            if (f.getTriggerTagsJson() != null) {
                List<String> triggers = JSON.parseArray(f.getTriggerTagsJson(), String.class);
                if (triggers != null) triggers.forEach(t -> triggerFreq.merge(t, 1, Integer::sum));
            }
            if (f.getCopingTagsJson() != null) {
                List<String> copings = JSON.parseArray(f.getCopingTagsJson(), String.class);
                if (copings != null) copings.forEach(c -> copingFreq.merge(c, 1, Integer::sum));
            }
            if (f.getSentimentScore() != null) {
                totalSentiment += f.getSentimentScore().doubleValue();
                sentimentCount++;
            }
        }

        summary.put("emotionDistribution", emotionFreq);
        summary.put("topTriggers", topN(triggerFreq, 5));
        summary.put("topCoping", topN(copingFreq, 5));
        summary.put("avgSentiment", sentimentCount > 0 ? BigDecimal.valueOf(totalSentiment / sentimentCount).setScale(2, java.math.RoundingMode.HALF_UP) : null);

        // 风险信号汇总
        long riskDiaryCount = features.stream().filter(f -> f.getRiskSignalJson() != null).count();
        summary.put("riskSignalCount", riskDiaryCount);

        return summary;
    }

    public List<MoodDiaryFeature> getPatientFeatureTimeline(Long patientId, LocalDateTime from, LocalDateTime to) {
        LambdaQueryWrapper<MoodDiaryFeature> w = new LambdaQueryWrapper<>();
        w.eq(MoodDiaryFeature::getUserId, patientId)
                .eq(MoodDiaryFeature::getExtractionStatus, "SUCCESS")
                .between(MoodDiaryFeature::getExtractedAt, from, to)
                .orderByAsc(MoodDiaryFeature::getExtractedAt);
        return featureMapper.selectList(w);
    }

    // ===== 批量补跑 =====

    @Scheduled(fixedDelay = 3600_000) // 每小时
    public void backfillPendingFeatures() {
        LambdaQueryWrapper<MoodDiary> w = new LambdaQueryWrapper<>();
        w.eq(MoodDiary::getFeatureStatus, "PENDING")
                .isNotNull(MoodDiary::getContent)
                .orderByAsc(MoodDiary::getCreateTime)
                .last("LIMIT 50");
        List<MoodDiary> pending = moodDiaryMapper.selectList(w);

        for (MoodDiary diary : pending) {
            try {
                extractDiaryFeatures(diary.getId());
            } catch (Exception e) {
                diary.setFeatureStatus("FAILED");
                moodDiaryMapper.updateById(diary);
                log.warn("日记#{} 特征抽取失败: {}", diary.getId(), e.getMessage());
            }
        }

        if (!pending.isEmpty()) {
            log.info("补跑日记特征: {}条", pending.size());
        }
    }

    // ===== 内部方法 =====

    private Map<String, Integer> matchDictionary(String content, Map<String, String[]> dict) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<String, String[]> entry : dict.entrySet()) {
            int count = 0;
            for (String keyword : entry.getValue()) {
                if (content.contains(keyword)) count++;
            }
            if (count > 0) result.put(entry.getKey(), count);
        }
        return result;
    }

    private BigDecimal calculateSentiment(Map<String, Integer> emotions, Integer moodScore) {
        int posWeight = 0, negWeight = 0;
        Set<String> positives = Set.of("JOY", "CALM", "HOPEFUL");
        Set<String> negatives = Set.of("SADNESS", "ANXIETY", "ANGER", "FATIGUE", "LONELY");

        for (Map.Entry<String, Integer> e : emotions.entrySet()) {
            if (positives.contains(e.getKey())) posWeight += e.getValue();
            if (negatives.contains(e.getKey())) negWeight += e.getValue();
        }

        double emotionScore = (posWeight - negWeight) / (double) Math.max(posWeight + negWeight, 1);
        double moodNorm = moodScore != null ? (moodScore - 5.0) / 5.0 : 0;
        double combined = emotionScore * 0.6 + moodNorm * 0.4;
        return BigDecimal.valueOf(Math.max(-1, Math.min(1, combined))).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private List<String> detectRiskSignals(String content) {
        List<String> signals = new ArrayList<>();
        String[] riskPatterns = {"自杀", "想死", "不想活", "伤害自己", "绝望", "没有希望", "活不下去"};
        for (String p : riskPatterns) {
            if (content.contains(p)) signals.add(p);
        }
        return signals;
    }

    private String generateInsightSummary(String primaryEmotion, Set<String> triggers, Set<String> copings, Integer moodScore) {
        StringBuilder sb = new StringBuilder();
        if (primaryEmotion != null) {
            sb.append("主要情绪: ").append(primaryEmotion);
        }
        if (moodScore != null) {
            sb.append(", 心情评分: ").append(moodScore).append("/10");
        }
        if (!triggers.isEmpty()) {
            sb.append("。触发因素: ").append(String.join("、", triggers));
        }
        if (!copings.isEmpty()) {
            sb.append("。应对方式: ").append(String.join("、", copings));
        }
        return sb.length() > 0 ? sb.toString() : "暂无洞察";
    }

    private List<Map.Entry<String, Integer>> topN(Map<String, Integer> freq, int n) {
        return freq.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(n)
                .toList();
    }
}
