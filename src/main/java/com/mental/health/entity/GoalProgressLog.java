package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("goal_progress_log")
public class GoalProgressLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long goalId;
    private Long loggedBy;
    private Integer progressPct;
    private String note;
    private LocalDate logDate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
