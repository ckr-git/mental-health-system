package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("treatment_plan_review")
public class TreatmentPlanReview {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long planId;
    private Long phaseId;
    private String reviewType;    // SCHEDULED,TRIGGERED,MANUAL,CRISIS
    private String reviewStatus;  // PENDING,IN_PROGRESS,COMPLETED,REOPENED
    private LocalDateTime dueAt;
    private Long reviewerId;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String conclusionCode; // CONTINUE,ADJUST,ESCALATE,REFER,DISCHARGE
    private String summaryText;
    private String actionPlanJson;
    private String triggerSourceType;
    private Long triggerSourceId;
    @Version
    private Integer version;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
