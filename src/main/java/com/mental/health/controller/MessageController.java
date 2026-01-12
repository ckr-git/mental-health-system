package com.mental.health.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mental.health.common.JwtUtil;
import com.mental.health.common.Result;
import com.mental.health.entity.Announcement;
import com.mental.health.entity.SystemAlert;
import com.mental.health.entity.SystemNotification;
import com.mental.health.entity.UserFeedback;
import com.mental.health.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 消息管理控制器
 */
@RestController
@RequestMapping("/api/admin/messages")
@PreAuthorize("hasRole('ADMIN')")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private JwtUtil jwtUtil;

    // ==================== Statistics ====================

    /**
     * Get message center statistics
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = messageService.getStatistics();
        return Result.success(stats);
    }

    // ==================== System Notifications ====================

    /**
     * Get notification list
     */
    @GetMapping("/notifications")
    public Result<IPage<SystemNotification>> getNotifications(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String notificationType,
            @RequestParam(required = false) Integer sendStatus) {
        IPage<SystemNotification> notifications = messageService.getNotifications(pageNum, pageSize, notificationType, sendStatus);
        return Result.success(notifications);
    }

    /**
     * Create notification
     */
    @PostMapping("/notifications")
    public Result<String> createNotification(@RequestBody SystemNotification notification) {
        boolean success = messageService.createNotification(notification);
        if (success) {
            return Result.success("创建成功");
        } else {
            return Result.error("创建失败");
        }
    }

    /**
     * Update notification
     */
    @PutMapping("/notifications/{id}")
    public Result<String> updateNotification(@PathVariable Long id, @RequestBody SystemNotification notification) {
        notification.setId(id);
        boolean success = messageService.updateNotification(notification);
        if (success) {
            return Result.success("更新成功");
        } else {
            return Result.error("更新失败");
        }
    }

    /**
     * Delete notification
     */
    @DeleteMapping("/notifications/{id}")
    public Result<String> deleteNotification(@PathVariable Long id) {
        boolean success = messageService.deleteNotification(id);
        if (success) {
            return Result.success("删除成功");
        } else {
            return Result.error("删除失败");
        }
    }

    /**
     * Send notification
     */
    @PostMapping("/notifications/{id}/send")
    public Result<String> sendNotification(@PathVariable Long id) {
        boolean success = messageService.sendNotification(id);
        if (success) {
            return Result.success("发送成功");
        } else {
            return Result.error("发送失败");
        }
    }

    // ==================== User Feedback ====================

    /**
     * Get feedback list
     */
    @GetMapping("/feedback")
    public Result<IPage<UserFeedback>> getFeedback(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String feedbackType,
            @RequestParam(required = false) Integer status) {
        IPage<UserFeedback> feedback = messageService.getFeedback(pageNum, pageSize, feedbackType, status);
        return Result.success(feedback);
    }

    /**
     * Get feedback detail
     */
    @GetMapping("/feedback/{id}")
    public Result<UserFeedback> getFeedbackDetail(@PathVariable Long id) {
        UserFeedback feedback = messageService.getFeedbackDetail(id);
        if (feedback != null) {
            return Result.success(feedback);
        } else {
            return Result.error("反馈不存在");
        }
    }

    /**
     * Reply to feedback
     */
    @PostMapping("/feedback/{id}/reply")
    public Result<String> replyFeedback(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            @RequestHeader("Authorization") String token) {
        String reply = request.get("reply");
        Long handlerId = jwtUtil.getUserIdFromToken(token.substring(7));

        boolean success = messageService.replyFeedback(id, reply, handlerId);
        if (success) {
            return Result.success("回复成功");
        } else {
            return Result.error("回复失败");
        }
    }

    /**
     * Update feedback status
     */
    @PutMapping("/feedback/{id}/status")
    public Result<String> updateFeedbackStatus(@PathVariable Long id, @RequestBody Map<String, Integer> request) {
        Integer status = request.get("status");
        boolean success = messageService.updateFeedbackStatus(id, status);
        if (success) {
            return Result.success("更新成功");
        } else {
            return Result.error("更新失败");
        }
    }

    /**
     * Delete feedback
     */
    @DeleteMapping("/feedback/{id}")
    public Result<String> deleteFeedback(@PathVariable Long id) {
        boolean success = messageService.deleteFeedback(id);
        if (success) {
            return Result.success("删除成功");
        } else {
            return Result.error("删除失败");
        }
    }

    // ==================== System Alerts ====================

    /**
     * Get alert list
     */
    @GetMapping("/alerts")
    public Result<IPage<SystemAlert>> getAlerts(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String alertType,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) Integer status) {
        IPage<SystemAlert> alerts = messageService.getAlerts(pageNum, pageSize, alertType, level, status);
        return Result.success(alerts);
    }

    /**
     * Get alert detail
     */
    @GetMapping("/alerts/{id}")
    public Result<SystemAlert> getAlertDetail(@PathVariable Long id) {
        SystemAlert alert = messageService.getAlertDetail(id);
        if (alert != null) {
            return Result.success(alert);
        } else {
            return Result.error("警报不存在");
        }
    }

    /**
     * Create alert
     */
    @PostMapping("/alerts")
    public Result<String> createAlert(@RequestBody SystemAlert alert) {
        boolean success = messageService.createAlert(alert);
        if (success) {
            return Result.success("创建成功");
        } else {
            return Result.error("创建失败");
        }
    }

    /**
     * Handle alert
     */
    @PostMapping("/alerts/{id}/handle")
    public Result<String> handleAlert(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            @RequestHeader("Authorization") String token) {
        String handleNote = (String) request.get("handleNote");
        Integer newStatus = (Integer) request.get("status");
        Long handlerId = jwtUtil.getUserIdFromToken(token.substring(7));

        boolean success = messageService.handleAlert(id, handleNote, handlerId, newStatus);
        if (success) {
            return Result.success("处理成功");
        } else {
            return Result.error("处理失败");
        }
    }

    /**
     * Delete alert
     */
    @DeleteMapping("/alerts/{id}")
    public Result<String> deleteAlert(@PathVariable Long id) {
        boolean success = messageService.deleteAlert(id);
        if (success) {
            return Result.success("删除成功");
        } else {
            return Result.error("删除失败");
        }
    }

    // ==================== Announcements ====================

    /**
     * Get announcement list
     */
    @GetMapping("/announcements")
    public Result<IPage<Announcement>> getAnnouncements(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String announcementType,
            @RequestParam(required = false) Integer status) {
        IPage<Announcement> announcements = messageService.getAnnouncements(pageNum, pageSize, announcementType, status);
        return Result.success(announcements);
    }

    /**
     * Get announcement detail
     */
    @GetMapping("/announcements/{id}")
    public Result<Announcement> getAnnouncementDetail(@PathVariable Long id) {
        Announcement announcement = messageService.getAnnouncementDetail(id);
        if (announcement != null) {
            return Result.success(announcement);
        } else {
            return Result.error("公告不存在");
        }
    }

    /**
     * Create announcement
     */
    @PostMapping("/announcements")
    public Result<String> createAnnouncement(@RequestBody Announcement announcement) {
        boolean success = messageService.createAnnouncement(announcement);
        if (success) {
            return Result.success("创建成功");
        } else {
            return Result.error("创建失败");
        }
    }

    /**
     * Update announcement
     */
    @PutMapping("/announcements/{id}")
    public Result<String> updateAnnouncement(@PathVariable Long id, @RequestBody Announcement announcement) {
        announcement.setId(id);
        boolean success = messageService.updateAnnouncement(announcement);
        if (success) {
            return Result.success("更新成功");
        } else {
            return Result.error("更新失败");
        }
    }

    /**
     * Publish announcement
     */
    @PostMapping("/announcements/{id}/publish")
    public Result<String> publishAnnouncement(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        Long publisherId = jwtUtil.getUserIdFromToken(token.substring(7));
        boolean success = messageService.publishAnnouncement(id, publisherId);
        if (success) {
            return Result.success("发布成功");
        } else {
            return Result.error("发布失败");
        }
    }

    /**
     * Unpublish announcement
     */
    @PostMapping("/announcements/{id}/unpublish")
    public Result<String> unpublishAnnouncement(@PathVariable Long id) {
        boolean success = messageService.unpublishAnnouncement(id);
        if (success) {
            return Result.success("下线成功");
        } else {
            return Result.error("下线失败");
        }
    }

    /**
     * Toggle announcement pin
     */
    @PostMapping("/announcements/{id}/toggle-pin")
    public Result<String> toggleAnnouncementPin(@PathVariable Long id) {
        boolean success = messageService.toggleAnnouncementPin(id);
        if (success) {
            return Result.success("操作成功");
        } else {
            return Result.error("操作失败");
        }
    }

    /**
     * Delete announcement
     */
    @DeleteMapping("/announcements/{id}")
    public Result<String> deleteAnnouncement(@PathVariable Long id) {
        boolean success = messageService.deleteAnnouncement(id);
        if (success) {
            return Result.success("删除成功");
        } else {
            return Result.error("删除失败");
        }
    }
}
