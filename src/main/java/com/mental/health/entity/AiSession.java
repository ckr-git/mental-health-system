package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_session")
public class AiSession {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long patientId;
    private String sessionType;   // SUPPORTIVE,PSYCHOEDUCATION,COPING_EXERCISE,CRISIS_SUPPORT
    private String sessionStatus; // ACTIVE,RISK_REVIEW_REQUIRED,HANDOFF_PENDING,HANDED_OFF,CLOSED
    private String topic;
    private Integer messageCount;
    private String riskLevel;     // NONE,LOW,MEDIUM,HIGH,CRITICAL
    private String riskFlagsJson;
    private LocalDateTime lastMessageAt;
    private Long handoffTaskId;
    private LocalDateTime closedAt;
    private String closeReason;   // USER_CLOSE,TIMEOUT,HANDOFF,SYSTEM
    @Version
    private Integer version;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
