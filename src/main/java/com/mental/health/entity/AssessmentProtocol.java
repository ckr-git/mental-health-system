package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("assessment_protocol")
public class AssessmentProtocol {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String protocolCode;
    private String protocolName;
    private String protocolType; // INTAKE,CRISIS_FOLLOWUP,TREATMENT_REVIEW,DISCHARGE,CUSTOM
    private String description;
    private String scaleCodesJson;
    private String triggerCondition; // MANUAL,ON_INTAKE,ON_CRISIS,ON_PLAN_REVIEW,PERIODIC
    private Integer periodDays;
    private String autoAssignFlag; // Y/N
    private Integer reminderBeforeHours;
    private Integer active;

    @Version
    private Integer version;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
