package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 天气配置实体类（系统配置表）
 */
@Data
@TableName("weather_config")
public class WeatherConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String weatherType;
    private String weatherName;

    // 心情范围
    private Integer moodMin;
    private Integer moodMax;

    // 视觉配置
    private String bgGradientStart;
    private String bgGradientEnd;
    private String weatherIcon;

    // 粒子配置
    private String particleType;
    private String particleColor;
    private Integer particleCount;
    private BigDecimal particleSpeed;

    // 音效
    private String ambientSound;

    // 描述
    private String description;
}
