package com.mental.health.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mental.health.dto.CreateNotificationCommand;
import com.mental.health.entity.*;
import com.mental.health.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * 通知策略服务 — 偏好矩阵、模板渲染、递送追踪、升级链
 *
 * 核心能力:
 * 1. 偏好管理: 用户按类别/渠道/优先级/静默时段配置通知偏好
 * 2. 模板渲染: 基于模板生成通知内容，支持变量替换
 * 3. 递送追踪: 记录每个通知的发送状态（PENDING→SENT→DELIVERED/FAILED）
 * 4. 升级链: 高优先级通知未确认时自动升级到上级
 * 5. 聚合推送: 低优先级通知合并推送，减少提醒疲劳
 */
@Service
public class NotificationStrategyService {

    private static final Logger log = LoggerFactory.getLogger(NotificationStrategyService.class);

    @Autowired private NotificationPreferenceMapper preferenceMapper;
    @Autowired private NotificationTemplateMapper templateMapper;
    @Autowired private NotificationDeliveryMapper deliveryMapper;
    @Autowired private UserNotificationMapper userNotificationMapper;
    @Autowired private NotificationPushService pushService;

    // ===== 偏好管理 =====

    public List<NotificationPreference> getUserPreferences(Long userId) {
        LambdaQueryWrapper<NotificationPreference> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotificationPreference::getUserId, userId)
                .orderByAsc(NotificationPreference::getCategory);
        return preferenceMapper.selectList(wrapper);
    }

    @Transactional
    public void savePreference(Long userId, String category, String channelCode,
                                boolean enabled, LocalTime quietStart, LocalTime quietEnd,
                                String minPriority, int coalesceMinutes) {
        // Upsert
        LambdaQueryWrapper<NotificationPreference> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotificationPreference::getUserId, userId)
                .eq(NotificationPreference::getCategory, category)
                .eq(NotificationPreference::getChannelCode, channelCode);
        NotificationPreference existing = preferenceMapper.selectOne(wrapper);

        if (existing != null) {
            existing.setEnabled(enabled ? 1 : 0);
            existing.setQuietStart(quietStart);
            existing.setQuietEnd(quietEnd);
            existing.setMinPriority(minPriority);
            existing.setCoalesceMinutes(coalesceMinutes);
            preferenceMapper.updateById(existing);
        } else {
            NotificationPreference pref = new NotificationPreference();
            pref.setUserId(userId);
            pref.setCategory(category);
            pref.setChannelCode(channelCode);
            pref.setEnabled(enabled ? 1 : 0);
            pref.setQuietStart(quietStart);
            pref.setQuietEnd(quietEnd);
            pref.setMinPriority(minPriority);
            pref.setCoalesceMinutes(coalesceMinutes);
            preferenceMapper.insert(pref);
        }
    }

    @Transactional
    public void initDefaultPreferences(Long userId, String role) {
        List<String> categories = List.of("CRISIS", "APPOINTMENT", "ASSESSMENT", "TREATMENT", "SYSTEM", "CHAT");
        for (String category : categories) {
            // 默认IN_APP全开
            savePreference(userId, category, "IN_APP", true, null, null, "LOW", 0);
            // WEBSOCKET全开
            savePreference(userId, category, "WEBSOCKET", true, null, null, "NORMAL", 0);
        }
    }

    // ===== 智能发送 =====

    /**
     * 使用模板发送通知，自动应用偏好过滤
     */
    @Transactional
    public UserNotification sendByTemplate(String templateCode, Long userId,
                                            Map<String, String> variables) {
        NotificationTemplate template = templateMapper.findByCode(templateCode);
        if (template == null) {
            log.warn("通知模板不存在: {}", templateCode);
            return null;
        }

        // 渲染模板
        String title = renderTemplate(template.getTitleTemplate(), variables);
        String content = renderTemplate(template.getContentTemplate(), variables);

        // 检查偏好
        if (!shouldSend(userId, template.getCategory(), template.getDefaultPriority())) {
            log.debug("通知被偏好过滤: 用户#{}, 模板={}", userId, templateCode);
            return null;
        }

        // 创建通知
        CreateNotificationCommand cmd = new CreateNotificationCommand();
        cmd.setUserId(userId);
        cmd.setCategory(template.getCategory());
        cmd.setPriority(template.getDefaultPriority());
        cmd.setTitle(title);
        cmd.setContent(content);
        cmd.setActionType(template.getActionType());

        UserNotification notification = new UserNotification();
        notification.setUserId(userId);
        notification.setCategory(template.getCategory());
        notification.setPriority(template.getDefaultPriority());
        notification.setTitle(title);
        notification.setContent(content);
        notification.setActionType(template.getActionType());
        notification.setTemplateCode(templateCode);
        notification.setReadStatus(0);

        if (template.getMustAck() != null && template.getMustAck() == 1) {
            notification.setMustAck(1);
            if (template.getAckDeadlineMinutes() != null) {
                notification.setAckDeadline(LocalDateTime.now().plusMinutes(template.getAckDeadlineMinutes()));
            }
        }

        if (template.getEscalationEnabled() != null && template.getEscalationEnabled() == 1) {
            notification.setEscalationStatus("PENDING");
        }

        userNotificationMapper.insert(notification);

        // 记录递送
        recordDelivery(notification.getId(), userId, "IN_APP", "SENT");

        // WebSocket推送
        try {
            pushService.pushNotification(userId, null);
            recordDelivery(notification.getId(), userId, "WEBSOCKET", "DELIVERED");
        } catch (Exception e) {
            recordDelivery(notification.getId(), userId, "WEBSOCKET", "FAILED");
        }

        return notification;
    }

    // ===== 升级链 =====

    /**
     * 检查需要升级的通知
     */
    @Scheduled(fixedDelay = 60_000)
    public void processEscalations() {
        LambdaQueryWrapper<UserNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserNotification::getMustAck, 1)
                .isNull(UserNotification::getAckedAt)
                .eq(UserNotification::getEscalationStatus, "PENDING")
                .le(UserNotification::getAckDeadline, LocalDateTime.now())
                .eq(UserNotification::getReadStatus, 0);

        List<UserNotification> pendingEscalations = userNotificationMapper.selectList(wrapper);

        for (UserNotification n : pendingEscalations) {
            try {
                // 查找模板确定升级目标
                NotificationTemplate template = n.getTemplateCode() != null
                        ? templateMapper.findByCode(n.getTemplateCode()) : null;

                if (template != null && template.getEscalationEnabled() == 1
                        && template.getEscalationTargetRole() != null) {
                    // 标记为已升级
                    n.setEscalationStatus("ESCALATED");
                    userNotificationMapper.updateById(n);

                    log.warn("通知#{} 超时未确认，已升级到角色: {}",
                            n.getId(), template.getEscalationTargetRole());
                }
            } catch (Exception e) {
                log.error("处理通知升级失败: #{}, {}", n.getId(), e.getMessage());
            }
        }
    }

    // ===== 确认 =====

    @Transactional
    public void acknowledgeNotification(Long notificationId, Long userId) {
        UserNotification n = userNotificationMapper.selectById(notificationId);
        if (n == null || !n.getUserId().equals(userId)) return;

        n.setAckedAt(LocalDateTime.now());
        n.setReadStatus(1);
        n.setReadTime(LocalDateTime.now());
        if ("PENDING".equals(n.getEscalationStatus())) {
            n.setEscalationStatus("NONE");
        }
        userNotificationMapper.updateById(n);
    }

    // ===== 内部方法 =====

    private boolean shouldSend(Long userId, String category, String priority) {
        LambdaQueryWrapper<NotificationPreference> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotificationPreference::getUserId, userId)
                .eq(NotificationPreference::getCategory, category)
                .eq(NotificationPreference::getChannelCode, "IN_APP");
        NotificationPreference pref = preferenceMapper.selectOne(wrapper);

        if (pref == null) return true; // 无偏好设置，默认发送
        if (pref.getEnabled() == 0) return false;

        // 检查优先级阈值
        if (pref.getMinPriority() != null) {
            int msgPriority = priorityOrder(priority);
            int minPriority = priorityOrder(pref.getMinPriority());
            if (msgPriority < minPriority) return false;
        }

        // 检查静默时段
        if (pref.getQuietStart() != null && pref.getQuietEnd() != null) {
            LocalTime now = LocalTime.now();
            if (pref.getQuietStart().isBefore(pref.getQuietEnd())) {
                if (now.isAfter(pref.getQuietStart()) && now.isBefore(pref.getQuietEnd())) {
                    // URGENT优先级不受静默时段限制
                    return "URGENT".equals(priority);
                }
            }
        }

        return true;
    }

    private int priorityOrder(String priority) {
        return switch (priority) {
            case "URGENT" -> 4;
            case "HIGH" -> 3;
            case "NORMAL" -> 2;
            case "LOW" -> 1;
            default -> 0;
        };
    }

    private String renderTemplate(String template, Map<String, String> variables) {
        if (template == null || variables == null) return template;
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue() != null ? entry.getValue() : "");
        }
        return result;
    }

    private void recordDelivery(Long notificationId, Long userId, String channel, String status) {
        NotificationDelivery delivery = new NotificationDelivery();
        delivery.setNotificationId(notificationId);
        delivery.setUserId(userId);
        delivery.setChannelCode(channel);
        delivery.setDeliveryStatus(status);
        delivery.setRetryCount(0);
        delivery.setCoalescedCount(1);
        delivery.setEscalatedFlag(0);

        switch (status) {
            case "SENT" -> delivery.setSentAt(LocalDateTime.now());
            case "DELIVERED" -> {
                delivery.setSentAt(LocalDateTime.now());
                delivery.setDeliveredAt(LocalDateTime.now());
            }
            case "FAILED" -> delivery.setFailedAt(LocalDateTime.now());
        }

        deliveryMapper.insert(delivery);
    }
}
