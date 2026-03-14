package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("risk_rule")
public class RiskRule implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String ruleCode;
    private String ruleName;
    private String sourceType;
    private String matcherType;
    private String keywordPattern;
    private String thresholdConfig;
    private String riskLevel;
    private Integer scoreWeight;
    private Integer cooldownMinutes;
    private Integer active;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
