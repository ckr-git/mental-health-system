package com.mental.health.service;

import com.mental.health.entity.UserThemeConfig;
import com.mental.health.mapper.UserThemeConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户主题配置服务
 */
@Service
public class UserThemeConfigService {

    @Autowired
    private UserThemeConfigMapper configMapper;

    /**
     * 获取用户配置（不存在则创建默认配置）
     */
    public UserThemeConfig getUserConfig(Long userId) {
        UserThemeConfig config = configMapper.getByUserId(userId);
        if (config == null) {
            config = createDefaultConfig(userId);
        }
        return config;
    }

    /**
     * 创建默认配置
     */
    @Transactional
    public UserThemeConfig createDefaultConfig(Long userId) {
        UserThemeConfig config = new UserThemeConfig();
        config.setUserId(userId);
        config.setCurrentTheme("default_day");
        config.setLightMode("day");
        config.setWeatherEnabled(1);
        config.setParticleEnabled(1);
        config.setAnimationEnabled(1);
        config.setSoundEnabled(1);
        config.setVolume(50);
        config.setLightToggleCount(0);
        config.setNightModeUsageCount(0);
        config.setTotalDiaryCount(0);
        config.setTotalCommentCount(0);
        config.setTotalLetterCount(0);
        config.setConsecutiveCheckInDays(0);
        config.setLowMoodSurvivedCount(0);

        // 默认解锁默认主题
        List<String> unlockedThemes = new ArrayList<>();
        unlockedThemes.add("default_day");
        config.setUnlockedThemes(unlockedThemes);

        configMapper.insert(config);
        return config;
    }

    /**
     * 切换灯光模式
     */
    @Transactional
    public boolean toggleLightMode(Long userId) {
        UserThemeConfig config = getUserConfig(userId);
        String newMode = "day".equals(config.getLightMode()) ? "night" : "day";
        config.setLightMode(newMode);

        Integer currentToggleCount = config.getLightToggleCount();
        config.setLightToggleCount(currentToggleCount == null ? 1 : currentToggleCount + 1);

        // 如果切换到夜晚模式，增加夜晚模式使用次数
        if ("night".equals(newMode)) {
            Integer currentNightCount = config.getNightModeUsageCount();
            config.setNightModeUsageCount(currentNightCount == null ? 1 : currentNightCount + 1);
        }

        return configMapper.updateById(config) > 0;
    }

    /**
     * 检查并解锁新主题
     */
    @Transactional
    public Map<String, Object> checkAndUnlockThemes(Long userId) {
        UserThemeConfig config = getUserConfig(userId);
        List<String> unlockedThemes = config.getUnlockedThemes();
        if (unlockedThemes == null) {
            unlockedThemes = new ArrayList<>();
            unlockedThemes.add("default_day");
        }

        List<String> newlyUnlocked = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        int currentMonth = now.getMonthValue();

        // 1. 圣诞主题 - 12月自动解锁
        if (currentMonth == 12 && !unlockedThemes.contains("christmas")) {
            unlockedThemes.add("christmas");
            newlyUnlocked.add("🎄 温馨圣诞屋");
        }

        // 2. 新年主题 - 1月解锁
        if (currentMonth == 1 && !unlockedThemes.contains("newyear")) {
            unlockedThemes.add("newyear");
            newlyUnlocked.add("🎆 新年烟花");
        }

        // 3. 万圣节主题 - 10月解锁
        if (currentMonth == 10 && !unlockedThemes.contains("halloween")) {
            unlockedThemes.add("halloween");
            newlyUnlocked.add("🎃 万圣节主题");
        }

        // 4. 樱花主题 - 连续30天打卡（null安全处理）
        Integer consecutiveDays = config.getConsecutiveCheckInDays();
        if (consecutiveDays != null && consecutiveDays >= 30 && !unlockedThemes.contains("cherry_blossom")) {
            unlockedThemes.add("cherry_blossom");
            newlyUnlocked.add("🌸 春日樱花房");
        }

        // 5. 海边小屋 - 完成10次时光信箱（null安全处理）
        Integer letterCount = config.getTotalLetterCount();
        if (letterCount != null && letterCount >= 10 && !unlockedThemes.contains("seaside")) {
            unlockedThemes.add("seaside");
            newlyUnlocked.add("🌊 海边小屋");
        }

        // 6. 山间木屋 - 度过5次低谷（null安全处理）
        Integer lowMoodCount = config.getLowMoodSurvivedCount();
        if (lowMoodCount != null && lowMoodCount >= 5 && !unlockedThemes.contains("mountain")) {
            unlockedThemes.add("mountain");
            newlyUnlocked.add("🏔️ 山间木屋");
        }

        // 7. 星空露营 - 夜晚模式使用30次（null安全处理）
        Integer nightModeCount = config.getNightModeUsageCount();
        if (nightModeCount != null && nightModeCount >= 30 && !unlockedThemes.contains("starry")) {
            unlockedThemes.add("starry");
            newlyUnlocked.add("🌙 星空露营");
        }

        // 更新已解锁主题列表
        if (!newlyUnlocked.isEmpty()) {
            config.setUnlockedThemes(unlockedThemes);
            configMapper.updateById(config);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("newlyUnlocked", newlyUnlocked);
        result.put("totalUnlocked", unlockedThemes.size());
        result.put("unlockedThemes", unlockedThemes);
        return result;
    }

    /**
     * 切换主题
     */
    @Transactional
    public boolean switchTheme(Long userId, String themeName) {
        UserThemeConfig config = getUserConfig(userId);
        List<String> unlockedThemes = config.getUnlockedThemes();

        // 检查主题是否已解锁
        if (unlockedThemes == null || !unlockedThemes.contains(themeName)) {
            return false;
        }

        config.setCurrentTheme(themeName);
        return configMapper.updateById(config) > 0;
    }

    /**
     * 更新配置
     */
    @Transactional
    public boolean updateConfig(UserThemeConfig config) {
        return configMapper.updateById(config) > 0;
    }

    /**
     * 增加统计计数
     */
    @Transactional
    public void incrementDiaryCount(Long userId) {
        UserThemeConfig config = getUserConfig(userId);
        Integer currentCount = config.getTotalDiaryCount();
        config.setTotalDiaryCount(currentCount == null ? 1 : currentCount + 1);
        configMapper.updateById(config);
    }

    @Transactional
    public void incrementCommentCount(Long userId) {
        UserThemeConfig config = getUserConfig(userId);
        Integer currentCount = config.getTotalCommentCount();
        config.setTotalCommentCount(currentCount == null ? 1 : currentCount + 1);
        configMapper.updateById(config);
    }

    @Transactional
    public void incrementLetterCount(Long userId) {
        UserThemeConfig config = getUserConfig(userId);
        Integer currentCount = config.getTotalLetterCount();
        config.setTotalLetterCount(currentCount == null ? 1 : currentCount + 1);
        configMapper.updateById(config);
    }

    @Transactional
    public void updateConsecutiveCheckInDays(Long userId, int days) {
        UserThemeConfig config = getUserConfig(userId);
        config.setConsecutiveCheckInDays(days);
        configMapper.updateById(config);
    }

    @Transactional
    public void incrementLowMoodSurvivedCount(Long userId) {
        UserThemeConfig config = getUserConfig(userId);
        Integer currentCount = config.getLowMoodSurvivedCount();
        config.setLowMoodSurvivedCount(currentCount == null ? 1 : currentCount + 1);
        configMapper.updateById(config);
    }
}
