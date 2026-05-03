package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("treatment_plan")
public class TreatmentPlan implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long patientId;
    private Long doctorId;
    private String title;
    private String diagnosis;
    /** DRAFT, ACTIVE, PAUSED, COMPLETED, ARCHIVED */
    private String planStatus;
    private Long currentPhaseId;
    private LocalDate startDate;
    private LocalDate targetEndDate;
    private LocalDate actualEndDate;
    private String summary;
    private String notes;
    private Integer planVersion;
    private Integer reviewIntervalDays;
    private LocalDateTime nextReviewAt;
    private java.math.BigDecimal adherenceScore;
    @Version
    private Integer version;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
