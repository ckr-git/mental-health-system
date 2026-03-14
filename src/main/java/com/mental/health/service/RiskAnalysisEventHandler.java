package com.mental.health.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mental.health.entity.OutboxEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class RiskAnalysisEventHandler implements OutboxEventHandler {

    private static final Set<String> SUPPORTED = Set.of(
            "DIARY_CREATED", "CHAT_MESSAGE_CREATED", "ASSESSMENT_COMPLETED");

    @Autowired
    private RiskAlertService riskAlertService;

    @Override
    public boolean supports(String eventType) {
        return SUPPORTED.contains(eventType);
    }

    @Override
    public void handle(OutboxEvent event) {
        JSONObject payload = JSON.parseObject(event.getPayload());
        if (payload == null) {
            log.warn("Empty payload for event: {}", event.getEventKey());
            return;
        }

        String sourceType = event.getEventType();
        Long sourceId = payload.getLong(resolveIdField(sourceType));
        Long userId = payload.getLong("userId");
        if (userId == null) {
            userId = payload.getLong("senderId");
        }
        if (userId == null) {
            log.warn("Cannot determine userId for event: {}", event.getEventKey());
            return;
        }

        String textContent = payload.getString("content");

        @SuppressWarnings("unchecked")
        Map<String, Object> structuredData = (Map<String, Object>) payload.to(Map.class);

        log.info("Risk analysis for event: type={}, userId={}", sourceType, userId);
        riskAlertService.analyzeAndHandle(sourceType, sourceId, userId, textContent, structuredData);
    }

    private String resolveIdField(String sourceType) {
        return switch (sourceType) {
            case "DIARY_CREATED" -> "diaryId";
            case "CHAT_MESSAGE_CREATED" -> "messageId";
            case "ASSESSMENT_COMPLETED" -> "sessionId";
            default -> "id";
        };
    }
}
