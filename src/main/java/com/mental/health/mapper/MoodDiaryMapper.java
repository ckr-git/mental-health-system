package com.mental.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mental.health.entity.MoodDiary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 情绪日记 Mapper
 */
@Mapper
public interface MoodDiaryMapper extends BaseMapper<MoodDiary> {

    /**
     * 获取用户最近的平均心情
     */
    @Select("SELECT AVG(mood_score) FROM mood_diary WHERE user_id = #{userId} AND create_time >= #{startTime}")
    Double getAvgMoodScore(@Param("userId") Long userId, @Param("startTime") LocalDateTime startTime);

    /**
     * 获取用户最新的一条日记
     */
    @Select("SELECT * FROM mood_diary WHERE user_id = #{userId} ORDER BY create_time DESC LIMIT 1")
    MoodDiary getLatestDiary(@Param("userId") Long userId);

    /**
     * 获取用户心情趋势数据
     */
    @Select("SELECT DATE(create_time) as date, AVG(mood_score) as avgMood FROM mood_diary " +
            "WHERE user_id = #{userId} AND create_time >= #{startTime} " +
            "GROUP BY DATE(create_time) ORDER BY date")
    List<Map<String, Object>> getMoodTrend(@Param("userId") Long userId, @Param("startTime") LocalDateTime startTime);

    /**
     * 获取用户各状态的日记数量
     */
    @Select("SELECT status, COUNT(*) as count FROM mood_diary WHERE user_id = #{userId} GROUP BY status")
    List<Map<String, Object>> getStatusStats(@Param("userId") Long userId);
}
