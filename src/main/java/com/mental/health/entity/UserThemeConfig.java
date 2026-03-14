package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户主题配置实体类
 */
@Data
@TableName(value = "user_theme_config", autoResultMap = true)
public class UserThemeConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    // 当前主题
    private String currentTheme;  // default_day, christmas, cherry_blossom, seaside, mountain, halloween, newyear, starry

    // 灯光模式
    private String lightMode;  // day, night

    // 已解锁的主题列表
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> unlockedThemes;

    // 效果开关
    private Integer weatherEnabled;
    private Integer particleEnabled;
    private Integer animationEnabled;

    // 音效设置
    private Integer soundEnabled;
    private Integer volume;

    // 统计数据
    private Integer lightToggleCount;
    private Integer nightModeUsageCount;  // 夜晚模式使用次数
    private Integer totalDiaryCount;
    private Integer totalCommentCount;
    private Integer totalLetterCount;
    private Integer consecutiveCheckInDays;  // 连续打卡天数
    private Integer lowMoodSurvivedCount;  // 度过的低谷次数

    // 时间戳
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
