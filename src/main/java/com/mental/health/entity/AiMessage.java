package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_message")
public class AiMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Long patientId;
    private String role;            // USER,ASSISTANT,SYSTEM
    private String content;
    private String riskDecisionJson;
    private String emotionTagsJson;
    private String responseType;    // NORMAL,SAFE_REDIRECT,CRISIS_PROMPT,HANDOFF_NOTICE
    private Integer tokenCount;
    private Integer latencyMs;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
