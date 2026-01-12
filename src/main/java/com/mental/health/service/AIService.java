package com.mental.health.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mental.health.entity.AIConversation;
import com.mental.health.entity.AssessmentReport;
import com.mental.health.entity.MoodDiary;
import com.mental.health.mapper.AIConversationMapper;
import com.mental.health.mapper.AssessmentReportMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI服务 - 智能问答和评估
 */
@Service
public class AIService {

    private static final Logger logger = LoggerFactory.getLogger(AIService.class);

    @Autowired
    private AIConversationMapper aiConversationMapper;

    @Autowired
    private AssessmentReportMapper assessmentReportMapper;

    @Autowired
    private MoodDiaryService moodDiaryService;

    @Value("${ai.api.url}")
    private String aiApiUrl;

    @Value("${ai.api.key}")
    private String aiApiKey;
    
    @Value("${ai.model}")
    private String aiModel;

    @Value("${ai.mock.enabled:true}")
    private boolean mockEnabled;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * AI问答
     */
    public String askQuestion(Long userId, String question) {
        // 获取用户历史记录作为上下文
        List<MoodDiary> recentRecords = moodDiaryService.getRecentDiaries(userId, 5);
        String context = buildContext(recentRecords);

        // 调用AI API
        String answer = callAIApi(question, context);

        // 保存对话记录
        AIConversation conversation = new AIConversation();
        conversation.setUserId(userId);
        conversation.setQuestion(question);
        conversation.setAnswer(answer);
        conversation.setContext(context);
        aiConversationMapper.insert(conversation);

        return answer;
    }

    /**
     * 调用阿里云百炼AI API
     */
    private String callAIApi(String question, String context) {
        // 如果启用Mock模式，使用模拟响应
        if (mockEnabled) {
            return generateMockResponse(question, context);
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + aiApiKey);

            // 构建阿里云百炼API请求格式
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", aiModel);

            // input字段
            Map<String, Object> input = new HashMap<>();
            Map<String, String>[] messages = new Map[2];
            messages[0] = new HashMap<>();
            messages[0].put("role", "system");
            messages[0].put("content", "你是一个专业、温暖、有同理心的心理健康助手。你的任务是倾听用户的困扰，提供专业的心理健康建议和支持。请用温和、鼓励的语气回复，避免使用过于专业的术语。用户背景：" + context);

            messages[1] = new HashMap<>();
            messages[1].put("role", "user");
            messages[1].put("content", question);

            input.put("messages", messages);
            requestBody.put("input", input);

            // parameters字段
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("result_format", "message");
            parameters.put("max_tokens", 800);
            parameters.put("temperature", 0.7);
            requestBody.put("parameters", parameters);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    aiApiUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JSONObject jsonResponse = JSON.parseObject(response.getBody());

                // 阿里云百炼API响应格式
                JSONObject output = jsonResponse.getJSONObject("output");
                if (output != null && output.containsKey("choices")) {
                    return output.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");
                }
            }
        } catch (Exception e) {
            logger.error("阿里云百炼API调用失败: {}", e.getMessage(), e);
            // 如果真实API调用失败，返回mock响应
            return generateMockResponse(question, context);
        }

        return "抱歉，我暂时无法回答您的问题。请稍后再试，或咨询专业的心理医生。";
    }
    
    /**
     * 生成模拟AI响应（用于演示和测试）
     */
    private String generateMockResponse(String question, String context) {
        String lowerQuestion = question.toLowerCase();
        
        // 根据关键词返回不同的响应
        if (lowerQuestion.contains("焦虑") || lowerQuestion.contains("紧张") || lowerQuestion.contains("anxiety")) {
            return "我理解您目前感到焦虑和紧张。首先，请您深呼吸，放松身体。焦虑是一种正常的情绪反应，以下是一些可以帮助您的建议：\n\n" +
                   "1. **深呼吸练习**：每天进行5-10分钟的腹式呼吸，可以有效缓解紧张感\n" +
                   "2. **规律作息**：保持良好的睡眠习惯，避免熬夜\n" +
                   "3. **适量运动**：每天30分钟的有氧运动可以释放内啡肽，改善心情\n" +
                   "4. **正念冥想**：尝试冥想app或课程，帮助您活在当下\n" +
                   "5. **寻求支持**：与信任的朋友或家人倾诉，或考虑专业心理咨询\n\n" +
                   "如果焦虑持续影响您的日常生活，建议您预约专业心理咨询师进行深入的评估和治疗。";
        }
        
        if (lowerQuestion.contains("抑郁") || lowerQuestion.contains("沮丧") || lowerQuestion.contains("悲伤") || lowerQuestion.contains("depression")) {
            return "我听到了您的感受，感到沮丧和悲伤是很艰难的体验。首先，我想告诉您，您并不孤单，这些感受是可以改善的。以下是一些建议：\n\n" +
                   "1. **建立支持系统**：与亲友保持联系，不要独自承受\n" +
                   "2. **设定小目标**：从简单的日常任务开始，逐步重建信心\n" +
                   "3. **身体活动**：即使是散步也能帮助改善心情\n" +
                   "4. **规律饮食和睡眠**：保持基本的生活规律\n" +
                   "5. **记录情绪日记**：写下您的感受可以帮助理清思绪\n" +
                   "6. **避免自我批判**：对自己温和一些\n\n" +
                   "重要的是，如果这些感受持续两周以上并严重影响生活，强烈建议您寻求专业心理咨询或精神科医生的帮助。必要时，药物治疗结合心理治疗会很有效。";
        }
        
        if (lowerQuestion.contains("睡眠") || lowerQuestion.contains("失眠") || lowerQuestion.contains("sleep") || lowerQuestion.contains("insomnia")) {
            return "睡眠问题会严重影响生活质量和心理健康。以下是改善睡眠的建议：\n\n" +
                   "1. **建立睡眠routine**：每天固定时间上床和起床，包括周末\n" +
                   "2. **优化睡眠环境**：保持卧室安静、黑暗、凉爽\n" +
                   "3. **限制屏幕时间**：睡前1小时避免使用手机、电脑\n" +
                   "4. **避免刺激物**：下午后避免咖啡因，睡前避免酒精和重餐\n" +
                   "5. **放松技巧**：尝试渐进式肌肉放松、冥想或轻柔音乐\n" +
                   "6. **白天适度运动**：但避免睡前3小时剧烈运动\n" +
                   "7. **处理忧虑**：睡前写下明天的待办事项，清空大脑\n\n" +
                   "如果失眠持续超过一个月，请咨询医生排除其他健康问题。";
        }
        
        if (lowerQuestion.contains("压力") || lowerQuestion.contains("stress") || lowerQuestion.contains("工作") || lowerQuestion.contains("学习")) {
            return "面对压力时感到不堪重负是很正常的。让我们一起找到管理压力的方法：\n\n" +
                   "1. **识别压力源**：明确是什么在困扰您\n" +
                   "2. **时间管理**：使用待办清单，优先处理重要事项\n" +
                   "3. **设定界限**：学会说不，不要过度承诺\n" +
                   "4. **定期休息**：每工作50分钟休息10分钟\n" +
                   "5. **培养爱好**：保留属于自己的放松时间\n" +
                   "6. **身心运动**：瑜伽、太极等结合身心的活动\n" +
                   "7. **社交支持**：与他人分享您的感受\n\n" +
                   "记住，适度的压力是动力，但长期过度压力需要及时调整。如果感到难以应对，请考虑寻求心理咨询师的专业指导。";
        }
        
        // 默认响应
        return "感谢您的信任。作为AI助手，我可以为您提供一些基本的心理健康建议，但我想提醒您：\n\n" +
               "1. **专业咨询的重要性**：对于严重或持续的心理健康问题，专业心理咨询师或精神科医生能提供更个性化和深入的帮助\n" +
               "2. **自我关怀**：请关注您的情绪、睡眠、饮食和运动习惯\n" +
               "3. **寻求支持**：不要独自承受，家人、朋友或专业人士都可以提供支持\n" +
               "4. **记录追踪**：使用本系统的症状记录功能，帮助您和医生更好地了解您的状况\n\n" +
               "您可以更具体地告诉我您的困扰，我会尽力提供更有针对性的建议。同时，我们的平台也可以帮您联系专业的心理医生。\n\n" +
               "如果您有自伤或伤害他人的想法，请立即拨打心理危机热线：400-161-9995（24小时）";
    }

    /**
     * 构建上下文
     */
    private String buildContext(List<MoodDiary> records) {
        if (records == null || records.isEmpty()) {
            return "用户是新用户，暂无历史记录。";
        }

        StringBuilder context = new StringBuilder("用户最近的心情日记：");
        for (MoodDiary record : records) {
            context.append("\n- 心情评分：").append(record.getMoodScore())
                    .append("，压力水平：").append(record.getStressLevel())
                    .append("，睡眠质量：").append(record.getSleepQuality())
                    .append("，精力水平：").append(record.getEnergyLevel());
            if (record.getContent() != null && !record.getContent().isEmpty()) {
                context.append("，内容：").append(record.getContent());
            }
        }

        return context.toString();
    }

    /**
     * 获取用户对话历史
     */
    public List<AIConversation> getUserConversations(Long userId, int limit) {
        LambdaQueryWrapper<AIConversation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AIConversation::getUserId, userId)
                .orderByDesc(AIConversation::getCreateTime)
                .last("LIMIT " + limit);
        return aiConversationMapper.selectList(queryWrapper);
    }

    /**
     * 用户反馈
     */
    public boolean submitFeedback(Long conversationId, Integer feedback) {
        AIConversation conversation = aiConversationMapper.selectById(conversationId);
        if (conversation != null) {
            conversation.setFeedback(feedback);
            return aiConversationMapper.updateById(conversation) > 0;
        }
        return false;
    }

    /**
     * 生成AI评估报告（增强版）
     */
    public String generateAssessmentReport(Long userId) {
        // 1. 获取用户最近30天的心情日记记录
        List<MoodDiary> records = moodDiaryService.getRecentDiaries(userId, 30);

        if (records.isEmpty()) {
            return "数据不足，无法生成评估报告。请至少记录7天的心情日记。";
        }

        // 2. 计算多维度评分
        Map<String, Integer> scores = calculateDimensionalScores(records);

        // 3. 生成详细的数据分析文本
        String dataAnalysis = buildDetailedAnalysis(records, scores);

        // 4. 调用AI生成专业评估报告
        String prompt = buildAssessmentPrompt(dataAnalysis, scores);
        String aiResponse = callAIApi(prompt, "生成心理健康评估报告");

        // 5. 解析AI响应，提取结构化内容
        Map<String, String> parsedReport = parseAIReport(aiResponse);

        // 6. 创建并保存评估报告实体
        AssessmentReport report = new AssessmentReport();
        report.setUserId(userId);
        report.setReportType("AI_COMPREHENSIVE");
        report.setTitle("心理健康综合评估报告");
        report.setContent(aiResponse);

        // 设置多维度评分
        report.setOverallScore(scores.get("overall"));
        report.setAnxietyScore(scores.get("anxiety"));
        report.setDepressionScore(scores.get("depression"));
        report.setStressScore(scores.get("stress"));
        report.setSleepScore(scores.get("sleep"));
        report.setEmotionScore(scores.get("emotion"));
        report.setSocialScore(scores.get("social"));

        // 设置分析结果
        report.setSummary(parsedReport.get("summary"));
        report.setDiagnosis(parsedReport.get("diagnosis"));
        report.setSuggestions(parsedReport.get("suggestions"));

        report.setIsAiGenerated(1);
        report.setStatus(1); // 已完成状态
        report.setCreateTime(LocalDateTime.now());
        report.setUpdateTime(LocalDateTime.now());

        // 保存报告
        assessmentReportMapper.insert(report);

        return "评估报告生成成功！报告ID：" + report.getId();
    }

    /**
     * 计算多维度评分（0-100分制）
     */
    private Map<String, Integer> calculateDimensionalScores(List<MoodDiary> records) {
        Map<String, Integer> scores = new HashMap<>();

        // 计算各维度平均值
        double avgMood = records.stream()
                .filter(r -> r.getMoodScore() != null)
                .mapToInt(MoodDiary::getMoodScore)
                .average().orElse(5);

        double avgStress = records.stream()
                .filter(r -> r.getStressLevel() != null)
                .mapToInt(MoodDiary::getStressLevel)
                .average().orElse(5);

        double avgSleep = records.stream()
                .filter(r -> r.getSleepQuality() != null)
                .mapToInt(MoodDiary::getSleepQuality)
                .average().orElse(5);

        double avgEnergy = records.stream()
                .filter(r -> r.getEnergyLevel() != null)
                .mapToInt(MoodDiary::getEnergyLevel)
                .average().orElse(5);

        // 情绪稳定性评分 (基于心情评分，0-10分转换为0-100分)
        int emotionScore = (int) (avgMood * 10);
        scores.put("emotion", Math.min(100, Math.max(0, emotionScore)));

        // 焦虑评分 (基于压力水平的逆向计算)
        int anxietyScore = (int) ((10 - avgStress) * 10);
        scores.put("anxiety", Math.min(100, Math.max(0, anxietyScore)));

        // 抑郁评分 (基于心情和精力的综合评估)
        int depressionScore = (int) ((avgMood * 0.6 + avgEnergy * 0.4) * 10);
        scores.put("depression", Math.min(100, Math.max(0, depressionScore)));

        // 压力管理评分
        int stressScore = (int) ((10 - avgStress) * 10);
        scores.put("stress", Math.min(100, Math.max(0, stressScore)));

        // 睡眠质量评分
        int sleepScore = (int) (avgSleep * 10);
        scores.put("sleep", Math.min(100, Math.max(0, sleepScore)));

        // 社交能力评分 (暂时基于整体状态估算，未来可以基于更多数据)
        int socialScore = (int) ((avgMood * 0.5 + avgEnergy * 0.5) * 10);
        scores.put("social", Math.min(100, Math.max(0, socialScore)));

        // 综合评分 (加权平均)
        int overallScore = (int) (
            emotionScore * 0.25 +
            depressionScore * 0.20 +
            stressScore * 0.20 +
            sleepScore * 0.15 +
            anxietyScore * 0.10 +
            socialScore * 0.10
        );
        scores.put("overall", Math.min(100, Math.max(0, overallScore)));

        return scores;
    }

    /**
     * 构建详细的数据分析文本
     */
    private String buildDetailedAnalysis(List<MoodDiary> records, Map<String, Integer> scores) {
        double avgMood = records.stream()
                .filter(r -> r.getMoodScore() != null)
                .mapToInt(MoodDiary::getMoodScore)
                .average().orElse(0);

        double avgStress = records.stream()
                .filter(r -> r.getStressLevel() != null)
                .mapToInt(MoodDiary::getStressLevel)
                .average().orElse(0);

        double avgSleep = records.stream()
                .filter(r -> r.getSleepQuality() != null)
                .mapToInt(MoodDiary::getSleepQuality)
                .average().orElse(0);

        double avgEnergy = records.stream()
                .filter(r -> r.getEnergyLevel() != null)
                .mapToInt(MoodDiary::getEnergyLevel)
                .average().orElse(0);

        // 统计心情状态分布
        Map<String, Long> statusCount = new HashMap<>();
        for (MoodDiary record : records) {
            String status = record.getStatus();
            if (status != null) {
                statusCount.put(status, statusCount.getOrDefault(status, 0L) + 1);
            }
        }

        StringBuilder analysis = new StringBuilder();
        analysis.append("【数据统计周期】").append(records.size()).append("天\n\n");
        analysis.append("【基础指标平均值】\n");
        analysis.append("- 心情评分：").append(String.format("%.1f", avgMood)).append("/10\n");
        analysis.append("- 压力水平：").append(String.format("%.1f", avgStress)).append("/10\n");
        analysis.append("- 睡眠质量：").append(String.format("%.1f", avgSleep)).append("/10\n");
        analysis.append("- 精力水平：").append(String.format("%.1f", avgEnergy)).append("/10\n\n");

        analysis.append("【多维度评分结果】\n");
        analysis.append("- 综合评分：").append(scores.get("overall")).append("/100\n");
        analysis.append("- 情绪稳定性：").append(scores.get("emotion")).append("/100\n");
        analysis.append("- 焦虑指数：").append(scores.get("anxiety")).append("/100\n");
        analysis.append("- 抑郁指数：").append(scores.get("depression")).append("/100\n");
        analysis.append("- 压力管理：").append(scores.get("stress")).append("/100\n");
        analysis.append("- 睡眠质量：").append(scores.get("sleep")).append("/100\n");
        analysis.append("- 社交能力：").append(scores.get("social")).append("/100\n\n");

        if (!statusCount.isEmpty()) {
            analysis.append("【心情状态分布】\n");
            statusCount.forEach((status, count) ->
                analysis.append("- ").append(getStatusName(status)).append("：").append(count).append("次\n")
            );
        }

        return analysis.toString();
    }

    /**
     * 获取状态中文名称
     */
    private String getStatusName(String status) {
        Map<String, String> statusMap = new HashMap<>();
        statusMap.put("ongoing", "正在经历");
        statusMap.put("better", "好转中");
        statusMap.put("overcome", "已克服");
        statusMap.put("proud", "为此自豪");
        return statusMap.getOrDefault(status, status);
    }

    /**
     * 构建AI评估提示词
     */
    private String buildAssessmentPrompt(String dataAnalysis, Map<String, Integer> scores) {
        return "你是一位专业的心理健康评估师。请根据以下患者的症状数据和评分结果，生成一份专业、客观、有温度的心理健康评估报告。\n\n" +
               dataAnalysis + "\n\n" +
               "请按照以下结构生成报告：\n\n" +
               "【评估摘要】\n" +
               "用2-3句话概括患者的整体心理健康状况。\n\n" +
               "【诊断分析】\n" +
               "基于数据，分析患者在情绪、焦虑、抑郁、压力、睡眠、社交等方面的具体表现，指出主要问题和积极方面。\n\n" +
               "【建议与指导】\n" +
               "提供3-5条具体、可操作的改善建议，包括生活方式调整、心理调节技巧、是否需要寻求专业帮助等。\n\n" +
               "请用专业但易懂的语言，保持客观和鼓励的态度。";
    }

    /**
     * 解析AI生成的报告，提取结构化内容
     */
    private Map<String, String> parseAIReport(String aiResponse) {
        Map<String, String> result = new HashMap<>();

        // 使用正则表达式提取各个部分
        String summary = extractSection(aiResponse, "【评估摘要】", "【诊断分析】");
        String diagnosis = extractSection(aiResponse, "【诊断分析】", "【建议与指导】");
        String suggestions = extractSection(aiResponse, "【建议与指导】", null);

        // 如果正则提取失败，使用整个响应作为摘要
        result.put("summary", summary != null && !summary.isEmpty() ? summary :
                   aiResponse.length() > 200 ? aiResponse.substring(0, 200) + "..." : aiResponse);
        result.put("diagnosis", diagnosis != null && !diagnosis.isEmpty() ? diagnosis : "请参考完整报告内容");
        result.put("suggestions", suggestions != null && !suggestions.isEmpty() ? suggestions : "请参考完整报告内容");

        return result;
    }

    /**
     * 从文本中提取指定章节内容
     */
    private String extractSection(String text, String startMarker, String endMarker) {
        try {
            int startIndex = text.indexOf(startMarker);
            if (startIndex == -1) return "";

            startIndex += startMarker.length();

            int endIndex = endMarker != null ? text.indexOf(endMarker, startIndex) : text.length();
            if (endIndex == -1) endIndex = text.length();

            return text.substring(startIndex, endIndex).trim();
        } catch (Exception e) {
            return "";
        }
    }
}
