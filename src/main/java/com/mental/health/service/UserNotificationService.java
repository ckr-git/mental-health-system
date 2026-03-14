package com.mental.health.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mental.health.dto.CreateNotificationCommand;
import com.mental.health.dto.UserNotificationDTO;
import com.mental.health.entity.UserNotification;
import com.mental.health.mapper.UserNotificationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class UserNotificationService {

    @Autowired
    private UserNotificationMapper userNotificationMapper;

    @Autowired
    private NotificationPushService notificationPushService;

    @Transactional
    public UserNotification createNotification(CreateNotificationCommand cmd) {
        UserNotification n = new UserNotification();
        n.setUserId(cmd.getUserId());
        n.setCategory(cmd.getCategory());
        n.setPriority(cmd.getPriority() != null ? cmd.getPriority() : "NORMAL");
        n.setTitle(cmd.getTitle());
        n.setContent(cmd.getContent());
        n.setActionType(cmd.getActionType());
        n.setActionPayload(cmd.getActionPayload());
        n.setSourceType(cmd.getSourceType());
        n.setSourceId(cmd.getSourceId());
        n.setReadStatus(0);
        userNotificationMapper.insert(n);

        notificationPushService.pushNotification(n.getUserId(), toDTO(n));
        return n;
    }

    public IPage<UserNotificationDTO> getNotifications(Long userId, int pageNum, int pageSize,
                                                        String category, Integer readStatus) {
        Page<UserNotification> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<UserNotification> w = new LambdaQueryWrapper<>();
        w.eq(UserNotification::getUserId, userId);
        if (StringUtils.hasText(category)) {
            w.eq(UserNotification::getCategory, category);
        }
        if (readStatus != null) {
            w.eq(UserNotification::getReadStatus, readStatus);
        }
        w.orderByDesc(UserNotification::getCreateTime);

        IPage<UserNotification> result = userNotificationMapper.selectPage(page, w);

        Page<UserNotificationDTO> dtoPage = new Page<>(pageNum, pageSize);
        dtoPage.setTotal(result.getTotal());
        dtoPage.setRecords(result.getRecords().stream().map(this::toDTO).toList());
        return dtoPage;
    }

    @Transactional
    public void markRead(Long userId, Long notificationId) {
        UserNotification n = userNotificationMapper.selectById(notificationId);
        if (n == null || !userId.equals(n.getUserId())) {
            throw new RuntimeException("通知不存在");
        }
        if (n.getReadStatus() == 0) {
            n.setReadStatus(1);
            n.setReadTime(LocalDateTime.now());
            userNotificationMapper.updateById(n);
        }
    }

    @Transactional
    public void markAllRead(Long userId) {
        userNotificationMapper.markAllRead(userId);
    }

    public Long getUnreadCount(Long userId) {
        return userNotificationMapper.countUnread(userId);
    }

    private UserNotificationDTO toDTO(UserNotification n) {
        UserNotificationDTO dto = new UserNotificationDTO();
        dto.setId(n.getId());
        dto.setCategory(n.getCategory());
        dto.setPriority(n.getPriority());
        dto.setTitle(n.getTitle());
        dto.setContent(n.getContent());
        dto.setActionType(n.getActionType());
        dto.setActionPayload(n.getActionPayload());
        dto.setSourceType(n.getSourceType());
        dto.setSourceId(n.getSourceId());
        dto.setReadStatus(n.getReadStatus());
        dto.setCreateTime(n.getCreateTime());
        return dto;
    }
}
