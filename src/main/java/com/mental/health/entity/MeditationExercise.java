package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("meditation_exercise")
public class MeditationExercise implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String exerciseCode;
    private String title;
    private String category;
    private String description;
    private Integer durationSeconds;
    private Integer inhaleSeconds;
    private Integer holdSeconds;
    private Integer exhaleSeconds;
    private Integer restSeconds;
    private String instructionSteps;
    private String animationPreset;
    private Integer active;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
