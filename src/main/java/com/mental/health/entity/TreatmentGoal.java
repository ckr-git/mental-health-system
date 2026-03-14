package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("treatment_goal")
public class TreatmentGoal implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long planId;
    /** SHORT_TERM or LONG_TERM */
    private String goalType;
    private String title;
    private String description;
    private String measurableTarget;
    private LocalDate targetDate;
    private Integer priority;
    /** PENDING, IN_PROGRESS, ACHIEVED, DEFERRED, DROPPED */
    private String status;
    private Integer progressPct;
    private LocalDateTime achievedAt;
    private Integer sortOrder;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
