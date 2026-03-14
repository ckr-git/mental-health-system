package com.mental.health.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mental.health.common.JwtUtil;
import com.mental.health.common.Result;
import com.mental.health.dto.UserNotificationDTO;
import com.mental.health.service.UserNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private UserNotificationService userNotificationService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping
    public Result<IPage<UserNotificationDTO>> getNotifications(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer readStatus,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        return Result.success(userNotificationService.getNotifications(userId, pageNum, pageSize, category, readStatus));
    }

    @PostMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id,
                                  @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        userNotificationService.markRead(userId, id);
        return Result.success(null);
    }

    @PostMapping("/read-all")
    public Result<Void> markAllRead(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        userNotificationService.markAllRead(userId);
        return Result.success(null);
    }

    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        return Result.success(userNotificationService.getUnreadCount(userId));
    }
}
