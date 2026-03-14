package com.mental.health.service;

import com.mental.health.entity.WeatherConfig;
import com.mental.health.mapper.WeatherConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 天气配置服务
 */
@Service
public class WeatherConfigService {

    @Autowired
    private WeatherConfigMapper weatherConfigMapper;

    /**
     * 获取所有天气配置
     */
    public List<WeatherConfig> getAllWeatherConfigs() {
        return weatherConfigMapper.selectList(null);
    }

    /**
     * 根据心情分数获取天气配置
     */
    public WeatherConfig getWeatherByMoodScore(Integer moodScore) {
        if (moodScore == null || moodScore < 1 || moodScore > 10) {
            moodScore = 5; // 默认值
        }
        return weatherConfigMapper.getByMoodScore(moodScore);
    }

    /**
     * 根据天气类型获取配置
     */
    public WeatherConfig getWeatherByType(String weatherType) {
        return weatherConfigMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WeatherConfig>()
                .eq(WeatherConfig::getWeatherType, weatherType)
        );
    }
}
