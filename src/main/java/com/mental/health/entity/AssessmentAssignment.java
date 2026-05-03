package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("assessment_assignment")
public class AssessmentAssignment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long patientId;
    private Long protocolId;
    private Long scaleId;
    private String scaleCode;
    private Long assignedBy;
    private LocalDateTime assignedAt;
    private LocalDateTime dueAt;
    private String assignmentStatus; // ASSIGNED,IN_PROGRESS,SUBMITTED,REVIEWED,OVERDUE,CLOSED,CANCELLED
    private Long sessionId;
    private Integer reminderSentCount;
    private LocalDateTime lastReminderAt;
    private String baselineFlag; // Y/N
    private Long treatmentPlanId;
    private String sourceEventType;
    private Long sourceEventId;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewNote;

    @Version
    private Integer version;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
