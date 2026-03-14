package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 情绪日记实体类
 */
@Data
@TableName("mood_diary")
public class MoodDiary implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    // 基础信息
    private Integer moodScore;
    private String moodEmoji;
    private String title;
    private String content;

    // 多维度评分
    private Integer energyLevel;
    private Integer sleepQuality;
    private Integer stressLevel;

    // 天气主题
    private String weatherType;
    private String weatherConfig;  // JSON字符串

    // 状态标记
    private String status;  // ongoing, better, overcome, proud
    private LocalDateTime statusUpdateTime;

    // 统计数据
    private Integer viewCount;
    private Integer commentCount;
    private String interactionCount;  // JSON字符串

    // 时间戳
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
