package com.mental.health.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mental.health.entity.Announcement;
import com.mental.health.entity.SystemAlert;
import com.mental.health.entity.SystemNotification;
import com.mental.health.entity.UserFeedback;
import com.mental.health.mapper.AnnouncementMapper;
import com.mental.health.mapper.SystemAlertMapper;
import com.mental.health.mapper.SystemNotificationMapper;
import com.mental.health.mapper.UserFeedbackMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 消息管理服务
 */
@Service
public class MessageService {

    @Autowired
    private SystemNotificationMapper notificationMapper;

    @Autowired
    private UserFeedbackMapper feedbackMapper;

    @Autowired
    private SystemAlertMapper alertMapper;

    @Autowired
    private AnnouncementMapper announcementMapper;

    // ==================== System Notifications ====================

    /**
     * Get paginated notifications
     */
    public IPage<SystemNotification> getNotifications(int pageNum, int pageSize, String notificationType, Integer sendStatus) {
        Page<SystemNotification> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SystemNotification> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(notificationType)) {
            wrapper.eq(SystemNotification::getNotificationType, notificationType);
        }
        if (sendStatus != null) {
            wrapper.eq(SystemNotification::getSendStatus, sendStatus);
        }

        wrapper.orderByDesc(SystemNotification::getCreateTime);
        return notificationMapper.selectPage(page, wrapper);
    }

    /**
     * Create notification
     */
    @Transactional
    public boolean createNotification(SystemNotification notification) {
        if (notification.getSendStatus() == null) {
            notification.setSendStatus(0); // Default: pending
        }
        if (notification.getReadStatus() == null) {
            notification.setReadStatus(0); // Default: unread
        }
        return notificationMapper.insert(notification) > 0;
    }

    /**
     * Update notification
     */
    @Transactional
    public boolean updateNotification(SystemNotification notification) {
        return notificationMapper.updateById(notification) > 0;
    }

    /**
     * Delete notification
     */
    @Transactional
    public boolean deleteNotification(Long id) {
        return notificationMapper.deleteById(id) > 0;
    }

    /**
     * Send notification
     */
    @Transactional
    public boolean sendNotification(Long id) {
        SystemNotification notification = notificationMapper.selectById(id);
        if (notification != null) {
            notification.setSendStatus(1); // Sent
            notification.setSentTime(LocalDateTime.now());
            return notificationMapper.updateById(notification) > 0;
        }
        return false;
    }

    // ==================== User Feedback ====================

    /**
     * Get paginated feedback
     */
    public IPage<UserFeedback> getFeedback(int pageNum, int pageSize, String feedbackType, Integer status) {
        Page<UserFeedback> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<UserFeedback> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(feedbackType)) {
            wrapper.eq(UserFeedback::getFeedbackType, feedbackType);
        }
        if (status != null) {
            wrapper.eq(UserFeedback::getStatus, status);
        }

        wrapper.orderByDesc(UserFeedback::getCreateTime);
        return feedbackMapper.selectPage(page, wrapper);
    }

    /**
     * Get feedback detail
     */
    public UserFeedback getFeedbackDetail(Long id) {
        return feedbackMapper.selectById(id);
    }

    /**
     * Reply to feedback
     */
    @Transactional
    public boolean replyFeedback(Long id, String reply, Long handlerId) {
        UserFeedback feedback = feedbackMapper.selectById(id);
        if (feedback != null) {
            feedback.setAdminReply(reply);
            feedback.setReplyTime(LocalDateTime.now());
            feedback.setHandlerId(handlerId);
            feedback.setStatus(2); // Completed
            return feedbackMapper.updateById(feedback) > 0;
        }
        return false;
    }

    /**
     * Update feedback status
     */
    @Transactional
    public boolean updateFeedbackStatus(Long id, Integer status) {
        UserFeedback feedback = feedbackMapper.selectById(id);
        if (feedback != null) {
            feedback.setStatus(status);
            return feedbackMapper.updateById(feedback) > 0;
        }
        return false;
    }

    /**
     * Delete feedback
     */
    @Transactional
    public boolean deleteFeedback(Long id) {
        return feedbackMapper.deleteById(id) > 0;
    }

    // ==================== System Alerts ====================

    /**
     * Get paginated alerts
     */
    public IPage<SystemAlert> getAlerts(int pageNum, int pageSize, String alertType, String level, Integer status) {
        Page<SystemAlert> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SystemAlert> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(alertType)) {
            wrapper.eq(SystemAlert::getAlertType, alertType);
        }
        if (StringUtils.hasText(level)) {
            wrapper.eq(SystemAlert::getLevel, level);
        }
        if (status != null) {
            wrapper.eq(SystemAlert::getStatus, status);
        }

        wrapper.orderByDesc(SystemAlert::getCreateTime);
        return alertMapper.selectPage(page, wrapper);
    }

    /**
     * Get alert detail
     */
    public SystemAlert getAlertDetail(Long id) {
        return alertMapper.selectById(id);
    }

    /**
     * Create alert
     */
    @Transactional
    public boolean createAlert(SystemAlert alert) {
        if (alert.getStatus() == null) {
            alert.setStatus(0); // Default: unhandled
        }
        return alertMapper.insert(alert) > 0;
    }

    /**
     * Handle alert
     */
    @Transactional
    public boolean handleAlert(Long id, String handleNote, Long handlerId, Integer newStatus) {
        SystemAlert alert = alertMapper.selectById(id);
        if (alert != null) {
            alert.setHandleNote(handleNote);
            alert.setHandlerId(handlerId);
            alert.setHandleTime(LocalDateTime.now());
            alert.setStatus(newStatus);
            return alertMapper.updateById(alert) > 0;
        }
        return false;
    }

    /**
     * Delete alert
     */
    @Transactional
    public boolean deleteAlert(Long id) {
        return alertMapper.deleteById(id) > 0;
    }

    // ==================== Announcements ====================

    /**
     * Get paginated announcements
     */
    public IPage<Announcement> getAnnouncements(int pageNum, int pageSize, String announcementType, Integer status) {
        Page<Announcement> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(announcementType)) {
            wrapper.eq(Announcement::getAnnouncementType, announcementType);
        }
        if (status != null) {
            wrapper.eq(Announcement::getStatus, status);
        }

        wrapper.orderByDesc(Announcement::getPinned)
               .orderByDesc(Announcement::getCreateTime);
        return announcementMapper.selectPage(page, wrapper);
    }

    /**
     * Get announcement detail
     */
    public Announcement getAnnouncementDetail(Long id) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement != null) {
            // Increment view count
            announcement.setViewCount((announcement.getViewCount() == null ? 0 : announcement.getViewCount()) + 1);
            announcementMapper.updateById(announcement);
        }
        return announcement;
    }

    /**
     * Create announcement
     */
    @Transactional
    public boolean createAnnouncement(Announcement announcement) {
        if (announcement.getStatus() == null) {
            announcement.setStatus(0); // Default: draft
        }
        if (announcement.getPinned() == null) {
            announcement.setPinned(0); // Default: not pinned
        }
        if (announcement.getViewCount() == null) {
            announcement.setViewCount(0);
        }
        return announcementMapper.insert(announcement) > 0;
    }

    /**
     * Update announcement
     */
    @Transactional
    public boolean updateAnnouncement(Announcement announcement) {
        return announcementMapper.updateById(announcement) > 0;
    }

    /**
     * Publish announcement
     */
    @Transactional
    public boolean publishAnnouncement(Long id, Long publisherId) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement != null) {
            announcement.setStatus(1); // Published
            announcement.setPublishTime(LocalDateTime.now());
            announcement.setPublisherId(publisherId);
            return announcementMapper.updateById(announcement) > 0;
        }
        return false;
    }

    /**
     * Unpublish announcement
     */
    @Transactional
    public boolean unpublishAnnouncement(Long id) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement != null) {
            announcement.setStatus(2); // Unpublished
            return announcementMapper.updateById(announcement) > 0;
        }
        return false;
    }

    /**
     * Toggle announcement pinned status
     */
    @Transactional
    public boolean toggleAnnouncementPin(Long id) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement != null) {
            announcement.setPinned(announcement.getPinned() == 1 ? 0 : 1);
            return announcementMapper.updateById(announcement) > 0;
        }
        return false;
    }

    /**
     * Delete announcement
     */
    @Transactional
    public boolean deleteAnnouncement(Long id) {
        return announcementMapper.deleteById(id) > 0;
    }

    // ==================== Statistics ====================

    /**
     * Get message center statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // Notification statistics
        LambdaQueryWrapper<SystemNotification> notifWrapper = new LambdaQueryWrapper<>();
        stats.put("totalNotifications", notificationMapper.selectCount(notifWrapper));
        notifWrapper.eq(SystemNotification::getSendStatus, 0);
        stats.put("pendingNotifications", notificationMapper.selectCount(notifWrapper));

        // Feedback statistics
        LambdaQueryWrapper<UserFeedback> feedbackWrapper = new LambdaQueryWrapper<>();
        stats.put("totalFeedback", feedbackMapper.selectCount(feedbackWrapper));
        feedbackWrapper.eq(UserFeedback::getStatus, 0);
        stats.put("pendingFeedback", feedbackMapper.selectCount(feedbackWrapper));

        // Alert statistics
        LambdaQueryWrapper<SystemAlert> alertWrapper = new LambdaQueryWrapper<>();
        stats.put("totalAlerts", alertMapper.selectCount(alertWrapper));
        alertWrapper.eq(SystemAlert::getStatus, 0);
        stats.put("unhandledAlerts", alertMapper.selectCount(alertWrapper));

        // Announcement statistics
        LambdaQueryWrapper<Announcement> announcementWrapper = new LambdaQueryWrapper<>();
        stats.put("totalAnnouncements", announcementMapper.selectCount(announcementWrapper));
        announcementWrapper.eq(Announcement::getStatus, 1);
        stats.put("publishedAnnouncements", announcementMapper.selectCount(announcementWrapper));

        return stats;
    }
}
