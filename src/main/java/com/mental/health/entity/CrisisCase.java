package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("crisis_case")
public class CrisisCase {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long patientId;
    private Long ownerDoctorId;
    private String caseStatus;  // NEW,TRIAGED,ACKED,CONTACTING,INTERVENING,ESCALATED,STABILIZED,RESOLVED,POST_REVIEW
    private String triageLevel; // MEDIUM,HIGH,CRITICAL
    private String caseSource;  // AUTO_RISK,MANUAL,ASSESSMENT,DIARY,CHAT
    private String title;
    private String summary;
    private Long initialRiskEventId;
    private Integer mergedEventCount;
    private Integer reopenCount;
    private LocalDateTime nextActionDeadline;
    private LocalDateTime slaDeadline;
    private String escalationChainJson;
    private LocalDateTime openedAt;
    private LocalDateTime triagedAt;
    private Long triagedBy;
    private LocalDateTime resolvedAt;
    private Long resolvedBy;
    private String resolutionCode;  // STABLE,REFERRED,HOSPITALIZED,FALSE_POSITIVE
    private String resolutionNote;
    private LocalDateTime postReviewDueAt;
    private LocalDateTime postReviewCompletedAt;

    @Version
    private Integer version;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
