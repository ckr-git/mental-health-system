package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("crisis_contact_attempt")
public class CrisisContactAttempt {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long crisisCaseId;
    private String contactTarget;   // PATIENT,EMERGENCY_CONTACT,DOCTOR,ADMIN
    private String contactChannel;  // PHONE,SMS,IN_APP,EMAIL
    private String contactName;
    private String contactInfo;
    private String attemptStatus;   // ATTEMPTED,REACHED,NO_ANSWER,FAILED
    private LocalDateTime attemptedAt;
    private Long attemptedBy;
    private Integer durationSeconds;
    private String outcomeNote;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
