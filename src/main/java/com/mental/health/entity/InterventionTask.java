package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("intervention_task")
public class InterventionTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long planId;
    private Long phaseId;
    private Long interventionId;
    private Long patientId;
    private Long assignedBy;
    private String taskType;      // HOMEWORK,MEDITATION,JOURNALING,EXERCISE,READING,CUSTOM
    private String title;
    private String description;
    private String taskStatus;    // ASSIGNED,IN_PROGRESS,SUBMITTED,REVIEWED,COMPLETED,OVERDUE,CANCELLED
    private LocalDateTime assignedAt;
    private LocalDateTime dueAt;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private String submissionNote;
    private String submissionValue;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewResultCode; // APPROVED,REDO,NOTED
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
