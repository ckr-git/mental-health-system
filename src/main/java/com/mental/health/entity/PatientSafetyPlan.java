package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("patient_safety_plan")
public class PatientSafetyPlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long patientId;
    private Long createdBy;
    private String planStatus;  // DRAFT,ACTIVE,ARCHIVED

    @TableField("warning_signs")
    private String warningSigns;

    @TableField("coping_strategies")
    private String copingStrategies;

    @TableField("support_contacts")
    private String supportContacts;

    private String safeEnvironment;

    @TableField("professional_contacts")
    private String professionalContacts;

    @TableField("crisis_hotlines")
    private String crisisHotlines;

    private String reasonsToLive;
    private LocalDateTime lastReviewedAt;
    private Long lastReviewedBy;

    @Version
    private Integer version;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
