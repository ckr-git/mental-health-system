package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("crisis_escalation_step")
public class CrisisEscalationStep {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long crisisCaseId;
    private Integer stepOrder;
    private String escalationType;  // TIMEOUT,MANUAL,RULE_TRIGGER,REOPEN
    private String fromStatus;
    private String toStatus;
    private Long fromDoctorId;
    private Long toDoctorId;
    private Integer timeoutMinutes;
    private LocalDateTime deadlineAt;
    private LocalDateTime executedAt;
    private Long executedBy;
    private String stepResult;  // SUCCESS,TIMEOUT,SKIPPED
    private String note;

    @Version
    private Integer version;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
