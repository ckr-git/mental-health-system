package com.mental.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mental.health.entity.WeatherConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 天气配置 Mapper
 */
@Mapper
public interface WeatherConfigMapper extends BaseMapper<WeatherConfig> {

    /**
     * 根据心情分数获取对应的天气配置
     */
    @Select("SELECT * FROM weather_config WHERE #{moodScore} BETWEEN mood_min AND mood_max LIMIT 1")
    WeatherConfig getByMoodScore(@Param("moodScore") Integer moodScore);
}
