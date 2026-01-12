package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 症状记录实体类
 */
@Data
@TableName("symptom_record")
public class SymptomRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String symptomType;
    private String description;
    private Integer severity;
    private Integer moodScore;
    private Integer energyLevel;
    private Integer sleepQuality;
    private Integer stressLevel;
    private String tags;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
