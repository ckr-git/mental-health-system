package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("session_note")
public class SessionNote implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long planId;
    private Long appointmentId;
    private Long patientId;
    private Long doctorId;
    private LocalDate sessionDate;
    /** INDIVIDUAL, GROUP, CRISIS, PHONE, ONLINE */
    private String sessionType;
    private String subjective;
    private String objective;
    private String assessment;
    private String planNotes;
    private String interventionsUsed; // JSON
    private String riskLevel;
    private String homework;
    private String nextSessionPlan;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
