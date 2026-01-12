package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 心情树洞实体类
 */
@Data
@TableName("tree_hole")
public class TreeHole implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    // 倾诉对象
    private String speakToType;  // self-自己, person-某人, role-角色, thing-某物, custom-自定义
    private String speakToName;  // 对象名称

    // 内容
    private String content;
    private String emotionTags;  // JSON数组
    private String contentType;  // text-文字, voice-语音, drawing-涂鸦
    private String attachmentUrl;

    // 时间设置
    private String expireType;  // 5sec, 1hour, tonight, tomorrow, forever, conditional
    private LocalDateTime expireTime;
    private Integer isExpired;  // 0-未消失, 1-已消失

    // 查看条件
    private String viewCondition;  // mood_low<3, mood_high>8, after_30days
    private Integer canView;  // 0-不可查看, 1-可查看

    // 状态
    private Integer moodAtWrite;  // 写入时心情评分
    private Integer viewCount;  // 查看次数
    private LocalDateTime lastViewTime;

    // 时间戳
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
