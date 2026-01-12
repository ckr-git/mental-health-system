package com.mental.health.controller;

import com.mental.health.common.Result;
import com.mental.health.entity.SystemSettings;
import com.mental.health.service.SystemSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统设置控制器
 */
@RestController
@RequestMapping("/api/admin/settings")
@PreAuthorize("hasRole('ADMIN')")
public class SystemSettingsController {

    @Autowired
    private SystemSettingsService settingsService;

    /**
     * Get all settings
     */
    @GetMapping
    public Result<List<SystemSettings>> getAllSettings() {
        List<SystemSettings> settings = settingsService.getAllSettings();
        return Result.success(settings);
    }

    /**
     * Get settings by group
     */
    @GetMapping("/grouped")
    public Result<Map<String, List<SystemSettings>>> getSettingsByGroup() {
        Map<String, List<SystemSettings>> settings = settingsService.getSettingsByGroup();
        return Result.success(settings);
    }

    /**
     * Get setting by key
     */
    @GetMapping("/{key}")
    public Result<SystemSettings> getSettingByKey(@PathVariable String key) {
        SystemSettings settings = settingsService.getSettingByKey(key);
        if (settings != null) {
            return Result.success(settings);
        } else {
            return Result.error("配置不存在");
        }
    }

    /**
     * Update setting
     */
    @PutMapping("/{key}")
    public Result<String> updateSetting(@PathVariable String key, @RequestBody Map<String, String> request) {
        String value = request.get("value");
        boolean success = settingsService.updateSetting(key, value);
        if (success) {
            return Result.success("更新成功");
        } else {
            return Result.error("更新失败或配置不可编辑");
        }
    }

    /**
     * Create setting
     */
    @PostMapping
    public Result<String> createSetting(@RequestBody SystemSettings settings) {
        boolean success = settingsService.createSetting(settings);
        if (success) {
            return Result.success("创建成功");
        } else {
            return Result.error("创建失败");
        }
    }

    /**
     * Delete setting
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteSetting(@PathVariable Long id) {
        boolean success = settingsService.deleteSetting(id);
        if (success) {
            return Result.success("删除成功");
        } else {
            return Result.error("删除失败或配置不可删除");
        }
    }
}
