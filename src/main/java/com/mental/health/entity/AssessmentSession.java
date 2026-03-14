package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("assessment_session")
public class AssessmentSession implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long scaleId;
    private String source;
    private String sessionStatus;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime scoredAt;
    private Integer totalScore;
    private String severityLevel;
    private String scoreBreakdown;
    private String interpretation;
    private Long reportId;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
