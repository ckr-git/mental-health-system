package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("referral_case")
public class ReferralCase {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long patientId;
    private Long fromDoctorId;
    private Long toDoctorId;
    private String referralStatus;  // INITIATED,ACCEPTED,HANDOFF_READY,HANDOFF_DONE,CLOSED,REJECTED,CANCELLED
    private String referralType;    // TRANSFER,CONSULTATION,SECOND_OPINION,SPECIALTY
    private String urgencyLevel;    // NORMAL,URGENT,EMERGENCY
    private String reason;
    private String clinicalSummary;
    private Long treatmentPlanId;
    private LocalDateTime initiatedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime rejectedAt;
    private String rejectReason;
    private LocalDateTime handoffReadyAt;
    private LocalDateTime handoffDoneAt;
    private LocalDateTime closedAt;
    private Long closedBy;
    private LocalDateTime deadlineAt;
    @Version
    private Integer version;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
