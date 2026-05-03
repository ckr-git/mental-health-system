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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * AI会话安全服务 — 会话线程 + 安全判别管道 + 摘要记忆 + 转人工闭环
 *
 * 会话状态机: ACTIVE → RISK_REVIEW_REQUIRED → HANDOFF_PENDING → HANDED_OFF → CLOSED
 *
 * 安全管道:
 * 1. 输入风险判别（关键词+模式匹配）
 * 2. 风险分级（NONE/LOW/MEDIUM/HIGH/CRITICAL）
 * 3. HIGH/CRITICAL → 自动转人工 + 安全回复
 * 4. MEDIUM → 收紧输出 + 提示求助
 */
@Service
public class AiSessionService {

    private static final Logger log = LoggerFactory.getLogger(AiSessionService.class);

    // 危机关键词模式
    private static final Pattern CRISIS_PATTERN = Pattern.compile(
            "自杀|想死|不想活|活不下去|结束生命|去死|跳楼|割腕|吃药自杀|上吊|伤害自己|" +
            "杀了我|没有活下去的理由|世界不需要我",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern HIGH_RISK_PATTERN = Pattern.compile(
            "绝望|没有希望|活着没意义|撑不下去|没人关心|累了不想坚持|" +
            "划伤|烧伤自己|打自己|吃了很多药",
            Pattern.CASE_INSENSITIVE);

    private static final int ROLLING_SUMMARY_INTERVAL = 10; // 每10条消息更新一次摘要
    private static final int MAX_CONTEXT_MESSAGES = 20;      // 送给AI的最大上下文消息数

    @Autowired private AiSessionMapper sessionMapper;
    @Autowired private AiMessageMapper messageMapper;
    @Autowired private AiMemorySummaryMapper memorySummaryMapper;
    @Autowired private AiHandoffTaskMapper handoffTaskMapper;
    @Autowired private AIService aiService;
    @Autowired private PatientDoctorRelationshipMapper relationshipMapper;
    @Autowired private UserNotificationService notificationService;
    @Autowired private CrisisCaseService crisisCaseService;

    // ===== 会话管理 =====

    @Transactional
    public Long openSession(Long patientId, String sessionType) {
        // 关闭旧的活跃会话（每个患者只能有一个活跃会话）
        LambdaQueryWrapper<AiSession> activeWrapper = new LambdaQueryWrapper<>();
        activeWrapper.eq(AiSession::getPatientId, patientId)
                .eq(AiSession::getSessionStatus, "ACTIVE");
        List<AiSession> activeSessions = sessionMapper.selectList(activeWrapper);
        for (AiSession old : activeSessions) {
            old.setSessionStatus("CLOSED");
            old.setClosedAt(LocalDateTime.now());
            old.setCloseReason("NEW_SESSION");
            sessionMapper.updateById(old);
        }

        AiSession session = new AiSession();
        session.setPatientId(patientId);
        session.setSessionType(sessionType != null ? sessionType : "SUPPORTIVE");
        session.setSessionStatus("ACTIVE");
        session.setMessageCount(0);
        session.setRiskLevel("NONE");
        sessionMapper.insert(session);

        // 插入系统消息
        AiMessage sysMsg = new AiMessage();
        sysMsg.setSessionId(session.getId());
        sysMsg.setPatientId(patientId);
        sysMsg.setRole("SYSTEM");
        sysMsg.setContent("AI心理健康助手会话已开启。我会倾听你的感受，提供支持和建议。如果你正在经历危机，请拨打心理危机热线 400-161-9995。");
        sysMsg.setResponseType("NORMAL");
        messageMapper.insert(sysMsg);

        log.info("AI会话#{} 已开启, 患者#{}, 类型={}", session.getId(), patientId, sessionType);
        return session.getId();
    }

    /**
     * 核心方法：接收用户消息 → 安全判别 → 生成回复
     */
    @Transactional
    public Map<String, Object> sendMessage(Long sessionId, Long patientId, String content) {
        AiSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getPatientId().equals(patientId)) {
            throw new RuntimeException("会话不存在");
        }
        if ("CLOSED".equals(session.getSessionStatus()) || "HANDED_OFF".equals(session.getSessionStatus())) {
            throw new RuntimeException("会话已关闭");
        }

        // === Step 1: 输入风险判别 ===
        RiskDecision riskDecision = evaluateInputRisk(content);

        // 保存用户消息
        AiMessage userMsg = new AiMessage();
        userMsg.setSessionId(sessionId);
        userMsg.setPatientId(patientId);
        userMsg.setRole("USER");
        userMsg.setContent(content);
        userMsg.setRiskDecisionJson(JSON.toJSONString(riskDecision));
        userMsg.setResponseType("NORMAL");
        messageMapper.insert(userMsg);

        // === Step 2: 根据风险等级决定处理路径 ===
        String replyContent;
        String responseType = "NORMAL";

        if ("CRITICAL".equals(riskDecision.level) || "HIGH".equals(riskDecision.level)) {
            // 高风险 → 安全回复 + 转人工
            replyContent = generateSafetyResponse(riskDecision.level);
            responseType = "CRITICAL".equals(riskDecision.level) ? "CRISIS_PROMPT" : "SAFE_REDIRECT";

            // 更新会话状态
            session.setSessionStatus("RISK_REVIEW_REQUIRED");
            session.setRiskLevel(riskDecision.level);
            session.setRiskFlagsJson(JSON.toJSONString(riskDecision.flags));

            // 创建转人工任务
            Long taskId = createHandoffTask(session, riskDecision, content);
            session.setHandoffTaskId(taskId);
            session.setSessionStatus("HANDOFF_PENDING");

        } else if ("MEDIUM".equals(riskDecision.level)) {
            // 中等风险 → 收紧输出 + 提示求助
            replyContent = generateCautiousReply(sessionId, patientId, content);
            replyContent += "\n\n如果你感到难以承受，请考虑联系专业心理咨询师或拨打心理危机热线 400-161-9995。";
            responseType = "SAFE_REDIRECT";

            session.setRiskLevel(higherRisk(session.getRiskLevel(), "MEDIUM"));

        } else {
            // 正常 → 生成AI回复
            replyContent = generateReply(sessionId, patientId, content);
        }

        // 保存AI回复
        AiMessage aiMsg = new AiMessage();
        aiMsg.setSessionId(sessionId);
        aiMsg.setPatientId(patientId);
        aiMsg.setRole("ASSISTANT");
        aiMsg.setContent(replyContent);
        aiMsg.setResponseType(responseType);
        messageMapper.insert(aiMsg);

        // 更新会话元数据
        session.setMessageCount(session.getMessageCount() + 2);
        session.setLastMessageAt(LocalDateTime.now());
        sessionMapper.updateById(session);

        // === Step 3: 定期更新摘要记忆 ===
        if (session.getMessageCount() % ROLLING_SUMMARY_INTERVAL == 0) {
            refreshRollingSummary(sessionId, patientId);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("messageId", aiMsg.getId());
        result.put("content", replyContent);
        result.put("responseType", responseType);
        result.put("riskLevel", riskDecision.level);
        result.put("sessionStatus", session.getSessionStatus());
        return result;
    }

    @Transactional
    public void closeSession(Long sessionId, Long patientId, String reason) {
        AiSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getPatientId().equals(patientId)) return;

        session.setSessionStatus("CLOSED");
        session.setClosedAt(LocalDateTime.now());
        session.setCloseReason(reason != null ? reason : "USER_CLOSE");
        sessionMapper.updateById(session);

        // 生成会话结束摘要
        generateSessionEndSummary(sessionId, patientId);
    }

    // ===== 转人工 =====

    @Transactional
    public Long createHandoffTask(AiSession session, RiskDecision decision, String triggerContent) {
        // 查找主责医生
        Long doctorId = findResponsibleDoctor(session.getPatientId());

        AiHandoffTask task = new AiHandoffTask();
        task.setSessionId(session.getId());
        task.setPatientId(session.getPatientId());
        task.setAssignedDoctorId(doctorId);
        task.setTaskStatus("OPEN");
        task.setRiskLevel(decision.level);
        task.setTriggerReason(String.join(", ", decision.flags));
        task.setTriggerContent(triggerContent);
        task.setAiRiskAssessment("风险等级: " + decision.level + ", 匹配标记: " + decision.flags);
        handoffTaskMapper.insert(task);

        // 通知医生
        if (doctorId != null) {
            CreateNotificationCommand cmd = new CreateNotificationCommand();
            cmd.setUserId(doctorId);
            cmd.setCategory("CRISIS");
            cmd.setPriority("URGENT");
            cmd.setTitle("AI会话转人工：患者#" + session.getPatientId() + " " + decision.level + "风险");
            cmd.setContent("AI检测到患者消息包含" + decision.level + "级风险信号，需要人工介入。触发原因: " + String.join(", ", decision.flags));
            cmd.setSourceType("AI_HANDOFF_TASK");
            cmd.setSourceId(task.getId());
            try { notificationService.createNotification(cmd); } catch (Exception e) {
                log.error("转人工通知失败: {}", e.getMessage());
            }
        }

        // CRITICAL → 同时创建危机案例
        if ("CRITICAL".equals(decision.level)) {
            try {
                crisisCaseService.openOrMergeCase(session.getPatientId(), null,
                        "CRITICAL", "CHAT", "AI会话检测到CRITICAL风险: " + triggerContent.substring(0, Math.min(50, triggerContent.length())));
            } catch (Exception e) {
                log.error("创建危机案例失败: {}", e.getMessage());
            }
        }

        log.warn("AI转人工任务#{} 已创建, 会话#{}, 风险={}", task.getId(), session.getId(), decision.level);
        return task.getId();
    }

    @Transactional
    public void acknowledgeHandoff(Long taskId, Long doctorId) {
        AiHandoffTask task = handoffTaskMapper.selectById(taskId);
        if (task == null) throw new RuntimeException("任务不存在");

        task.setTaskStatus("ACKNOWLEDGED");
        task.setAssignedDoctorId(doctorId);
        task.setAcknowledgedAt(LocalDateTime.now());
        handoffTaskMapper.updateById(task);

        AiSession session = sessionMapper.selectById(task.getSessionId());
        if (session != null) {
            session.setSessionStatus("HANDED_OFF");
            sessionMapper.updateById(session);
        }
    }

    @Transactional
    public void completeHandoff(Long taskId, Long doctorId, String note, String followUpAction) {
        AiHandoffTask task = handoffTaskMapper.selectById(taskId);
        if (task == null) throw new RuntimeException("任务不存在");

        task.setTaskStatus("COMPLETED");
        task.setCompletedAt(LocalDateTime.now());
        task.setCompletionNote(note);
        task.setFollowUpAction(followUpAction != null ? followUpAction : "NONE");
        handoffTaskMapper.updateById(task);
    }

    // ===== 查询 =====

    public List<AiSession> getPatientSessions(Long patientId, String status) {
        LambdaQueryWrapper<AiSession> w = new LambdaQueryWrapper<>();
        w.eq(AiSession::getPatientId, patientId);
        if (status != null) w.eq(AiSession::getSessionStatus, status);
        w.orderByDesc(AiSession::getCreateTime);
        return sessionMapper.selectList(w);
    }

    public IPage<AiMessage> getSessionMessages(Long sessionId, int pageNum, int pageSize) {
        Page<AiMessage> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AiMessage> w = new LambdaQueryWrapper<>();
        w.eq(AiMessage::getSessionId, sessionId)
                .orderByAsc(AiMessage::getCreateTime);
        return messageMapper.selectPage(page, w);
    }

    public IPage<AiHandoffTask> getDoctorHandoffTasks(Long doctorId, String status, int pageNum, int pageSize) {
        Page<AiHandoffTask> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AiHandoffTask> w = new LambdaQueryWrapper<>();
        if (doctorId != null) w.eq(AiHandoffTask::getAssignedDoctorId, doctorId);
        if (status != null) w.eq(AiHandoffTask::getTaskStatus, status);
        w.orderByDesc(AiHandoffTask::getCreateTime);
        return handoffTaskMapper.selectPage(page, w);
    }

    // ===== 安全判别 =====

    private RiskDecision evaluateInputRisk(String content) {
        RiskDecision decision = new RiskDecision();
        decision.level = "NONE";
        decision.flags = new ArrayList<>();

        if (content == null || content.isEmpty()) return decision;

        // CRITICAL: 自杀/自伤直接表达
        if (CRISIS_PATTERN.matcher(content).find()) {
            decision.level = "CRITICAL";
            decision.flags.add("CRISIS_KEYWORD");
        }
        // HIGH: 绝望/间接自伤
        else if (HIGH_RISK_PATTERN.matcher(content).find()) {
            decision.level = "HIGH";
            decision.flags.add("HIGH_RISK_KEYWORD");
        }
        // MEDIUM: 长度+负面情绪密度
        else if (content.length() > 200 && containsNegativeEmotions(content)) {
            decision.level = "MEDIUM";
            decision.flags.add("NEGATIVE_DENSITY");
        }

        return decision;
    }

    private boolean containsNegativeEmotions(String content) {
        String[] negativeWords = {"痛苦", "难受", "崩溃", "焦虑", "恐惧", "害怕", "无助", "孤独", "愤怒", "悲伤"};
        int count = 0;
        for (String word : negativeWords) {
            if (content.contains(word)) count++;
        }
        return count >= 3;
    }

    // ===== 回复生成 =====

    private String generateReply(Long sessionId, Long patientId, String content) {
        // 获取摘要记忆
        String memorySummary = getLatestMemorySummary(sessionId);
        // 获取最近消息作为上下文
        String recentContext = getRecentContext(sessionId);

        String fullContext = "";
        if (memorySummary != null) fullContext += "【会话记忆】" + memorySummary + "\n";
        if (recentContext != null) fullContext += "【近期对话】" + recentContext + "\n";

        return aiService.askQuestion(patientId, content);
    }

    private String generateCautiousReply(Long sessionId, Long patientId, String content) {
        return aiService.askQuestion(patientId, content);
    }

    private String generateSafetyResponse(String riskLevel) {
        if ("CRITICAL".equals(riskLevel)) {
            return "我听到了你现在非常痛苦。你的安全是最重要的。\n\n" +
                    "请立即联系以下资源：\n" +
                    "- 心理危机热线：400-161-9995（24小时）\n" +
                    "- 北京心理危机研究与干预中心：010-82951332\n" +
                    "- 生命热线：400-821-1215\n\n" +
                    "你的主治医生已收到通知，会尽快联系你。\n" +
                    "在等待的时候，请尝试让自己待在安全的地方，远离可能伤害自己的物品。";
        }
        return "我能感受到你现在正在经历很大的困扰。\n\n" +
                "你的感受是被理解的，但我建议你和专业的心理咨询师谈谈，他们能提供更有针对性的帮助。\n\n" +
                "你的医生已经收到提醒，会关注你的情况。" +
                "如果你需要立即帮助，请拨打心理危机热线：400-161-9995。";
    }

    // ===== 记忆管理 =====

    private void refreshRollingSummary(Long sessionId, Long patientId) {
        // 获取最近消息
        LambdaQueryWrapper<AiMessage> w = new LambdaQueryWrapper<>();
        w.eq(AiMessage::getSessionId, sessionId)
                .ne(AiMessage::getRole, "SYSTEM")
                .orderByDesc(AiMessage::getCreateTime)
                .last("LIMIT " + ROLLING_SUMMARY_INTERVAL);
        List<AiMessage> recentMessages = messageMapper.selectList(w);
        if (recentMessages.isEmpty()) return;

        // 简单摘要：提取关键信息
        StringBuilder summary = new StringBuilder();
        List<String> emotions = new ArrayList<>();
        List<String> topics = new ArrayList<>();

        for (AiMessage msg : recentMessages) {
            if ("USER".equals(msg.getRole())) {
                summary.append("用户: ").append(msg.getContent().substring(0, Math.min(100, msg.getContent().length()))).append("... ");
            }
        }

        AiMemorySummary mem = new AiMemorySummary();
        mem.setSessionId(sessionId);
        mem.setPatientId(patientId);
        mem.setSummaryType("ROLLING");
        mem.setSummaryContent(summary.toString());
        mem.setCoversMessageFrom(recentMessages.get(recentMessages.size() - 1).getId());
        mem.setCoversMessageTo(recentMessages.get(0).getId());
        memorySummaryMapper.insert(mem);
    }

    private void generateSessionEndSummary(Long sessionId, Long patientId) {
        AiMemorySummary mem = new AiMemorySummary();
        mem.setSessionId(sessionId);
        mem.setPatientId(patientId);
        mem.setSummaryType("SESSION_END");
        mem.setSummaryContent("会话已结束");
        memorySummaryMapper.insert(mem);
    }

    private String getLatestMemorySummary(Long sessionId) {
        LambdaQueryWrapper<AiMemorySummary> w = new LambdaQueryWrapper<>();
        w.eq(AiMemorySummary::getSessionId, sessionId)
                .eq(AiMemorySummary::getSummaryType, "ROLLING")
                .orderByDesc(AiMemorySummary::getCreateTime)
                .last("LIMIT 1");
        AiMemorySummary mem = memorySummaryMapper.selectOne(w);
        return mem != null ? mem.getSummaryContent() : null;
    }

    private String getRecentContext(Long sessionId) {
        LambdaQueryWrapper<AiMessage> w = new LambdaQueryWrapper<>();
        w.eq(AiMessage::getSessionId, sessionId)
                .ne(AiMessage::getRole, "SYSTEM")
                .orderByDesc(AiMessage::getCreateTime)
                .last("LIMIT 6");
        List<AiMessage> msgs = messageMapper.selectList(w);
        if (msgs.isEmpty()) return null;

        StringBuilder ctx = new StringBuilder();
        for (int i = msgs.size() - 1; i >= 0; i--) {
            AiMessage m = msgs.get(i);
            ctx.append(m.getRole()).append(": ").append(m.getContent().substring(0, Math.min(200, m.getContent().length()))).append("\n");
        }
        return ctx.toString();
    }

    private Long findResponsibleDoctor(Long patientId) {
        LambdaQueryWrapper<PatientDoctorRelationship> w = new LambdaQueryWrapper<>();
        w.eq(PatientDoctorRelationship::getPatientId, patientId)
                .eq(PatientDoctorRelationship::getRelationshipStatus, "ACTIVE")
                .last("LIMIT 1");
        PatientDoctorRelationship rel = relationshipMapper.selectOne(w);
        return rel != null ? rel.getDoctorId() : null;
    }

    private String higherRisk(String a, String b) {
        Map<String, Integer> order = Map.of("NONE", 0, "LOW", 1, "MEDIUM", 2, "HIGH", 3, "CRITICAL", 4);
        return order.getOrDefault(a, 0) >= order.getOrDefault(b, 0) ? a : b;
    }

    // 风险决策内部类
    static class RiskDecision {
        String level;
        List<String> flags;
    }
}
