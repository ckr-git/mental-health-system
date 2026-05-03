package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@TableName("appointment_slot_hold")
public class AppointmentSlotHold {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long doctorId;
    private Long patientId;
    private LocalDate slotDate;
    private LocalTime slotStart;
    private LocalTime slotEnd;
    private String holdStatus; // HELD,CONVERTED,RELEASED,EXPIRED
    private String holdSource; // WAITLIST_OFFER,BOOKING_IN_PROGRESS
    private Long sourceId;
    private LocalDateTime heldAt;
    private LocalDateTime expireAt;
    private LocalDateTime convertedAt;
    private Long resultingAppointmentId;

    @Version
    private Integer version;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
