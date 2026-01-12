package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI对话记录实体类
 */
@Data
@TableName("ai_conversation")
public class AIConversation implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String question;
    private String answer;
    private String context;
    private Integer feedback;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
