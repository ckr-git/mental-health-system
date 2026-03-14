package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("treatment_intervention")
public class TreatmentIntervention implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long planId;
    private Long goalId;
    /** CBT, MEDICATION, EXERCISE, HOMEWORK, MEDITATION, JOURNALING, OTHER */
    private String interventionType;
    private String title;
    private String description;
    private String frequency;
    /** ACTIVE, COMPLETED, DROPPED */
    private String status;
    private Integer sortOrder;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
