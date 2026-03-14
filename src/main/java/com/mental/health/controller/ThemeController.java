package com.mental.health.controller;

import com.mental.health.common.JwtUtil;
import com.mental.health.common.Result;
import com.mental.health.entity.UserThemeConfig;
import com.mental.health.entity.WeatherConfig;
import com.mental.health.service.UserThemeConfigService;
import com.mental.health.service.WeatherConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 主题配置控制器
 */
@RestController
@RequestMapping("/api/patient/theme")
@PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
public class ThemeController {

    @Autowired
    private UserThemeConfigService themeConfigService;

    @Autowired
    private WeatherConfigService weatherConfigService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 获取当前主题配置
     */
    @GetMapping("/config")
    public Result<UserThemeConfig> getConfig(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        UserThemeConfig config = themeConfigService.getUserConfig(userId);
        return Result.success(config);
    }

    /**
     * 检查并解锁新主题
     */
    @PostMapping("/check-unlock")
    public Result<Map<String, Object>> checkUnlock(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        Map<String, Object> result = themeConfigService.checkAndUnlockThemes(userId);
        return Result.success(result);
    }

    /**
     * 切换主题
     */
    @PostMapping("/switch")
    public Result<String> switchTheme(
            @RequestBody Map<String, String> request,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        String themeName = request.get("themeName");

        if (themeName == null || themeName.isEmpty()) {
            return Result.error("主题名称不能为空");
        }

        boolean success = themeConfigService.switchTheme(userId, themeName);
        if (success) {
            return Result.success("主题切换成功");
        }
        return Result.error("主题未解锁或切换失败");
    }

    /**
     * 获取已解锁主题列表
     */
    @GetMapping("/unlocked")
    public Result<Map<String, Object>> getUnlockedThemes(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        UserThemeConfig config = themeConfigService.getUserConfig(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("currentTheme", config.getCurrentTheme());
        result.put("unlockedThemes", config.getUnlockedThemes());
        result.put("lightMode", config.getLightMode());

        return Result.success(result);
    }

    /**
     * 切换灯光模式
     */
    @PostMapping("/toggle-light")
    public Result<Map<String, String>> toggleLight(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        boolean success = themeConfigService.toggleLightMode(userId);

        if (success) {
            UserThemeConfig config = themeConfigService.getUserConfig(userId);
            Map<String, String> result = new HashMap<>();
            result.put("lightMode", config.getLightMode());
            result.put("message", "day".equals(config.getLightMode()) ? "开灯啦 💡" : "关灯啦 🌙");
            return Result.success(result);
        }
        return Result.error("切换失败");
    }

    /**
     * 更新设置
     */
    @PutMapping("/settings")
    public Result<String> updateSettings(@RequestBody UserThemeConfig config, @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        config.setUserId(userId);

        boolean success = themeConfigService.updateConfig(config);
        if (success) {
            return Result.success("设置更新成功");
        }
        return Result.error("设置更新失败");
    }

    /**
     * 获取所有天气配置
     */
    @GetMapping("/weather")
    public Result<List<WeatherConfig>> getWeatherConfigs() {
        List<WeatherConfig> configs = weatherConfigService.getAllWeatherConfigs();
        return Result.success(configs);
    }

    /**
     * 根据心情获取天气
     */
    @GetMapping("/weather/{moodScore}")
    public Result<WeatherConfig> getWeatherByMood(@PathVariable Integer moodScore) {
        WeatherConfig weather = weatherConfigService.getWeatherByMoodScore(moodScore);
        if (weather != null) {
            return Result.success(weather);
        }
        return Result.error("天气配置不存在");
    }
}
