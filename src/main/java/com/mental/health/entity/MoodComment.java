package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 心情留言实体类
 */
@Data
@TableName("mood_comment")
public class MoodComment implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long diaryId;
    private Long userId;

    // 留言内容
    private String content;
    private String commentType;  // random, praise, hug, note, thought

    // 互动标记（JSON数组）
    private String interactions;  // JSON字符串: ["agree", "heartache"]

    // 情绪对比
    private Integer moodAtComment;
    private Integer moodAtDiary;
    private Integer moodGap;

    // 时间戳
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
