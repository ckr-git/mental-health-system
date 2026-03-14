package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("doctor_performance")
public class DoctorPerformance implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long doctorId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Integer totalPatients;
    private Integer activePlans;
    private Integer completedPlans;
    private Integer appointmentsScheduled;
    private Integer appointmentsCompleted;
    private BigDecimal avgPatientImprovement;
    private Integer crisisAlertsHandled;
    private Integer sessionNotesCount;
    private BigDecimal avgResponseTimeHours;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
