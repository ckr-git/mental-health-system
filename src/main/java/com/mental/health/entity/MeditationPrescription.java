package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("meditation_prescription")
public class MeditationPrescription {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long patientId;
    private Long doctorId;
    private Long treatmentPlanId;
    private Long exerciseId;
    private String prescriptionStatus; // DRAFT,ACTIVE,PAUSED,COMPLETED,CANCELLED
    private String goalCode;           // ANXIETY_RELIEF,SLEEP_SUPPORT,GROUNDING,MAINTENANCE
    private String frequencyCode;      // DAILY,WEEKLY,CUSTOM
    private Integer sessionsPerWeek;
    private Integer minutesPerSession;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private LocalDateTime nextDueAt;
    private Integer totalSessionsCompleted;
    private BigDecimal avgEffectScore;
    @Version
    private Integer version;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
