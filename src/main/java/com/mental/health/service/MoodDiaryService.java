package com.mental.health.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mental.health.entity.MoodDiary;
import com.mental.health.entity.WeatherConfig;
import com.mental.health.mapper.MoodDiaryMapper;
import com.mental.health.mapper.WeatherConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 情绪日记服务
 */
@Service
public class MoodDiaryService {

    @Autowired
    private MoodDiaryMapper moodDiaryMapper;

    @Autowired
    private WeatherConfigMapper weatherConfigMapper;

    /**
     * 添加日记
     */
    @Transactional
    public MoodDiary addDiary(MoodDiary diary) {
        // 自动设置天气类型
        if (diary.getMoodScore() != null) {
            WeatherConfig weather = weatherConfigMapper.getByMoodScore(diary.getMoodScore());
            if (weather != null) {
                diary.setWeatherType(weather.getWeatherType());
                // 构建天气配置JSON
                Map<String, Object> config = new HashMap<>();
                config.put("bgStart", weather.getBgGradientStart());
                config.put("bgEnd", weather.getBgGradientEnd());
                config.put("particleType", weather.getParticleType());
                config.put("particleColor", weather.getParticleColor());
                config.put("particleCount", weather.getParticleCount());
                config.put("particleSpeed", weather.getParticleSpeed());
                diary.setWeatherConfig(JSON.toJSONString(config));
            }
        }

        // 设置默认值
        if (diary.getStatus() == null) {
            diary.setStatus("ongoing");
        }
        if (diary.getViewCount() == null) {
            diary.setViewCount(0);
        }
        if (diary.getCommentCount() == null) {
            diary.setCommentCount(0);
        }

        // 初始化互动统计
        Map<String, Integer> interactions = new HashMap<>();
        interactions.put("agree", 0);
        interactions.put("disagree", 0);
        interactions.put("heartache", 0);
        interactions.put("encourage", 0);
        interactions.put("relief", 0);
        diary.setInteractionCount(JSON.toJSONString(interactions));

        moodDiaryMapper.insert(diary);
        return diary;
    }

    /**
     * 获取日记列表（分页）
     */
    public Page<MoodDiary> getDiaryList(Long userId, int pageNum, int pageSize) {
        Page<MoodDiary> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<MoodDiary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MoodDiary::getUserId, userId)
                .orderByDesc(MoodDiary::getCreateTime);
        return moodDiaryMapper.selectPage(page, wrapper);
    }

    /**
     * 获取日记详情
     */
    @Transactional
    public MoodDiary getDiaryDetail(Long id, Long userId) {
        MoodDiary diary = moodDiaryMapper.selectById(id);
        if (diary != null && diary.getUserId().equals(userId)) {
            // 增加查看次数
            diary.setViewCount(diary.getViewCount() + 1);
            moodDiaryMapper.updateById(diary);
            return diary;
        }
        return null;
    }

    /**
     * 更新状态
     */
    @Transactional
    public boolean updateStatus(Long id, Long userId, String status) {
        MoodDiary diary = moodDiaryMapper.selectById(id);
        if (diary != null && diary.getUserId().equals(userId)) {
            diary.setStatus(status);
            diary.setStatusUpdateTime(LocalDateTime.now());
            return moodDiaryMapper.updateById(diary) > 0;
        }
        return false;
    }

    /**
     * 获取用户统计数据
     */
    public Map<String, Object> getUserStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        
        // 总数
        LambdaQueryWrapper<MoodDiary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MoodDiary::getUserId, userId);
        Long total = moodDiaryMapper.selectCount(wrapper);
        stats.put("totalDiaries", total);

        // 近7天平均心情
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        Double avgMood = moodDiaryMapper.getAvgMoodScore(userId, sevenDaysAgo);
        stats.put("avgMood7Days", avgMood != null ? avgMood : 0.0);

        // 近30天平均心情
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        Double avgMood30 = moodDiaryMapper.getAvgMoodScore(userId, thirtyDaysAgo);
        stats.put("avgMood30Days", avgMood30 != null ? avgMood30 : 0.0);

        // 各状态数量
        List<Map<String, Object>> statusStats = moodDiaryMapper.getStatusStats(userId);
        stats.put("statusStats", statusStats);

        // 心情趋势（近30天）
        List<Map<String, Object>> moodTrend = moodDiaryMapper.getMoodTrend(userId, thirtyDaysAgo);
        stats.put("moodTrend", moodTrend);

        return stats;
    }

    /**
     * 获取最近的日记列表（不分页）
     */
    public List<MoodDiary> getRecentDiaries(Long userId, int limit) {
        LambdaQueryWrapper<MoodDiary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MoodDiary::getUserId, userId)
                .orderByDesc(MoodDiary::getCreateTime)
                .last("LIMIT " + limit);
        return moodDiaryMapper.selectList(wrapper);
    }
}
