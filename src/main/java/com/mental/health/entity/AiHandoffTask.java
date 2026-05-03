package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_handoff_task")
public class AiHandoffTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Long patientId;
    private Long assignedDoctorId;
    private String taskStatus;      // OPEN,ACKNOWLEDGED,IN_PROGRESS,COMPLETED,CANCELLED
    private String riskLevel;
    private String triggerReason;
    private String triggerContent;
    private String aiRiskAssessment;
    private String sessionSummary;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime completedAt;
    private String completionNote;
    private String followUpAction;  // NONE,SCHEDULE_APPOINTMENT,CREATE_CRISIS_CASE,REFER
    @Version
    private Integer version;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
