package com.mental.health.controller;

import com.mental.health.common.JwtUtil;
import com.mental.health.common.Result;
import com.mental.health.entity.NotificationPreference;
import com.mental.health.service.NotificationStrategyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class NotificationPreferenceController {

    @Autowired private NotificationStrategyService strategyService;
    @Autowired private JwtUtil jwtUtil;

    @GetMapping("/user/notification-preferences")
    public Result<List<NotificationPreference>> getPreferences(
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        return Result.success(strategyService.getUserPreferences(userId));
    }

    @PostMapping("/user/notification-preferences")
    public Result<Void> savePreference(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> body) {
        Long userId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        String category = (String) body.get("category");
        String channelCode = (String) body.get("channelCode");
        boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
        LocalTime quietStart = body.get("quietStart") != null
                ? LocalTime.parse(body.get("quietStart").toString()) : null;
        LocalTime quietEnd = body.get("quietEnd") != null
                ? LocalTime.parse(body.get("quietEnd").toString()) : null;
        String minPriority = (String) body.getOrDefault("minPriority", "LOW");
        int coalesceMinutes = body.get("coalesceMinutes") != null
                ? Integer.parseInt(body.get("coalesceMinutes").toString()) : 0;

        strategyService.savePreference(userId, category, channelCode,
                enabled, quietStart, quietEnd, minPriority, coalesceMinutes);
        return Result.success();
    }

    @PostMapping("/user/notification-preferences/init-defaults")
    public Result<Void> initDefaults(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        strategyService.initDefaultPreferences(userId, null);
        return Result.success();
    }

    @PostMapping("/user/notifications/{notificationId}/acknowledge")
    public Result<Void> acknowledge(
            @RequestHeader("Authorization") String token,
            @PathVariable Long notificationId) {
        Long userId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        strategyService.acknowledgeNotification(notificationId, userId);
        return Result.success();
    }
}
