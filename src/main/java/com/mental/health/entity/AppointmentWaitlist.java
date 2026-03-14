package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("appointment_waitlist")
public class AppointmentWaitlist implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long patientId;

    private Long doctorId;

    private LocalDate preferredDate;

    private LocalTime preferredTimeStart;

    private LocalTime preferredTimeEnd;

    private String appointmentType;

    private String symptoms;

    /** WAITING, NOTIFIED, BOOKED, EXPIRED, CANCELLED */
    private String status;

    private LocalDateTime notifiedAt;

    private LocalDateTime expiredAt;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
