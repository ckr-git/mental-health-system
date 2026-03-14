package com.mental.health.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mental.health.entity.SystemSettings;
import com.mental.health.mapper.SystemSettingsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统设置服务
 */
@Service
public class SystemSettingsService {

    @Autowired
    private SystemSettingsMapper settingsMapper;

    /**
     * Get all settings
     */
    public List<SystemSettings> getAllSettings() {
        return settingsMapper.selectList(new LambdaQueryWrapper<>());
    }

    /**
     * Get settings by group
     */
    public Map<String, List<SystemSettings>> getSettingsByGroup() {
        List<SystemSettings> allSettings = getAllSettings();
        return allSettings.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getGroupName() != null ? s.getGroupName() : "DEFAULT"
                ));
    }

    /**
     * Get setting by key
     */
    public SystemSettings getSettingByKey(String key) {
        LambdaQueryWrapper<SystemSettings> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemSettings::getConfigKey, key);
        return settingsMapper.selectOne(wrapper);
    }

    /**
     * Update setting
     */
    @Transactional
    public boolean updateSetting(String key, String value) {
        SystemSettings settings = getSettingByKey(key);
        if (settings != null && settings.getEditable() == 1) {
            settings.setConfigValue(value);
            return settingsMapper.updateById(settings) > 0;
        }
        return false;
    }

    /**
     * Create setting
     */
    @Transactional
    public boolean createSetting(SystemSettings settings) {
        if (settings.getEditable() == null) {
            settings.setEditable(1);
        }
        return settingsMapper.insert(settings) > 0;
    }

    /**
     * Delete setting
     */
    @Transactional
    public boolean deleteSetting(Long id) {
        SystemSettings settings = settingsMapper.selectById(id);
        if (settings != null && settings.getEditable() == 1) {
            return settingsMapper.deleteById(id) > 0;
        }
        return false;
    }
}
