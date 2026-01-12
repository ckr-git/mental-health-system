package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评估报告实体类
 */
@Data
@TableName("assessment_report")
public class AssessmentReport implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long doctorId;
    private String reportType;
    private String title;
    private String content;
    private String diagnosis;
    private String suggestions;

    // 多维度评分字段
    private Integer overallScore;
    private Integer anxietyScore;
    private Integer depressionScore;
    private Integer stressScore;
    private Integer sleepScore;
    private Integer emotionScore;
    private Integer socialScore;
    private String summary;

    private Integer isAiGenerated;
    private Integer status;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
