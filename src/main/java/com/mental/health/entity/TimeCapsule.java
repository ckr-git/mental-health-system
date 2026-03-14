package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 时光信箱实体类
 */
@Data
@TableName("time_capsule")
public class TimeCapsule implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    // 信件信息
    private String letterType;  // praise, hope, thanks
    private String title;
    private String content;

    // 触发数据
    private BigDecimal triggerMoodAvg;
    private String triggerMoodTrend;  // up, stable, down
    private String relatedDiaryIds;  // JSON数组

    // 时间控制
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime writeDate;
    private LocalDate unlockDate;
    private Integer unlockDays;
    private String unlockType;  // days, date, condition
    private String unlockConditions;  // JSON数组

    // 状态
    private String status;  // sealed, unlocked, read, replied
    private LocalDateTime unlockTime;
    private LocalDateTime readTime;
    private String replyContent;
    private LocalDateTime replyTime;

    // 时间戳
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
