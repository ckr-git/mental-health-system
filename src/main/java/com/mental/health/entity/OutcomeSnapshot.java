package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("outcome_snapshot")
public class OutcomeSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long patientId;
    private Long doctorId;
    private LocalDate snapshotDate;
    private String snapshotType;

    private Integer phq9Score;
    private Integer gad7Score;
    private String phq9Severity;
    private String gad7Severity;

    private BigDecimal avgMoodScore;
    private BigDecimal avgSleepQuality;
    private BigDecimal avgStressLevel;
    private BigDecimal avgEnergyLevel;
    private Integer diaryCount;

    private Integer appointmentCount;
    private Integer appointmentAttended;
    private Integer meditationMinutes;
    private Integer assessmentCount;

    private Long activePlanId;
    private Integer goalsTotal;
    private Integer goalsAchieved;
    private Integer avgGoalProgress;

    private Integer riskEventsCount;
    private Integer crisisAlertsCount;

    private String clinicianNotes;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
