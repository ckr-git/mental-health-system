package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("assessment_baseline")
public class AssessmentBaseline {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long patientId;
    private String scaleCode;
    private Long baselineSessionId;
    private Long treatmentPlanId;
    private Integer totalScore;
    private String dimensionScoresJson;
    private String severityLevel;
    private LocalDateTime establishedAt;
    private LocalDateTime supersededAt;
    private Long supersededBy;

    @Version
    private Integer version;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
