package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("doctor_schedule")
public class DoctorSchedule implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long doctorId;

    /** 1=Monday, 7=Sunday */
    private Integer dayOfWeek;

    private LocalTime startTime;

    private LocalTime endTime;

    /** minutes per slot */
    private Integer slotDuration;

    private Integer maxPatients;

    private String location;

    private Integer active;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
