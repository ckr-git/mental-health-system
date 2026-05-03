package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("patient_daily_metric")
public class PatientDailyMetric {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long patientId;
    private LocalDateTime metricAt;
    private String metricStatus; // CALCULATED,LOCKED,RECALCULATED
    private BigDecimal moodAvg;
    private BigDecimal sleepAvg;
    private BigDecimal stressAvg;
    private BigDecimal energyAvg;
    private Integer diaryCount;
    private Integer latestPhq9Score;
    private Integer latestGad7Score;
    private BigDecimal taskCompletionRate;
    private Integer meditationMinutes;
    private Integer riskEventCount;
    private Integer noShowCount;
    private LocalDateTime sourceWindowStart;
    private LocalDateTime sourceWindowEnd;
    @Version
    private Integer version;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
