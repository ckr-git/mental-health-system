package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("appointment_reschedule_log")
public class AppointmentRescheduleLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long appointmentId;
    private LocalDateTime originalTime;
    private LocalDateTime newTime;
    private String rescheduleReason;
    private Long rescheduledBy;
    private String rescheduledByRole; // PATIENT,DOCTOR,ADMIN
    private Long rescheduledFromId;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
