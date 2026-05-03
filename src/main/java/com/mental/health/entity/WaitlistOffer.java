package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@TableName("waitlist_offer")
public class WaitlistOffer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long waitlistId;
    private Long patientId;
    private Long doctorId;
    private LocalDate offeredSlotDate;
    private LocalTime offeredSlotStart;
    private LocalTime offeredSlotEnd;
    private String offerStatus; // PENDING,ACCEPTED,DECLINED,EXPIRED,CANCELLED
    private Integer priorityScore;
    private String priorityFactorsJson;
    private LocalDateTime offeredAt;
    private LocalDateTime expireAt;
    private LocalDateTime respondedAt;
    private String declineReason;
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
