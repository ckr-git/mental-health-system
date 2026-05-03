package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("treatment_plan_phase")
public class TreatmentPlanPhase {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long planId;
    private String phaseCode;    // INTAKE,STABILIZATION,ACTIVE_WORK,MAINTENANCE,DISCHARGE
    private String phaseName;
    private Integer phaseOrder;
    private String phaseStatus;  // PLANNED,ACTIVE,COMPLETED,CANCELLED,SKIPPED
    private String entryCriteria;
    private String exitCriteria;
    private String requiredAssessmentsJson;
    private String requiredInterventionsJson;
    private String maxRiskLevel;
    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;
    private LocalDate actualStartDate;
    private LocalDate actualEndDate;
    private Long activatedBy;
    private Long completedBy;
    private String completionNote;
    @Version
    private Integer version;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
