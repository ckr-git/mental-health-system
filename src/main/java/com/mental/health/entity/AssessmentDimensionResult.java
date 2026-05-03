package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("assessment_dimension_result")
public class AssessmentDimensionResult {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;
    private Long patientId;
    private String scaleCode;
    private String dimensionCode;
    private String dimensionName;
    private Integer rawScore;
    private BigDecimal normalizedScore;
    private String severityLevel;
    private String itemIdsJson;
    private BigDecimal changeFromBaseline;
    private String clinicalSignificance; // IMPROVED,STABLE,DETERIORATED,CLINICALLY_SIGNIFICANT

    @Version
    private Integer version;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
