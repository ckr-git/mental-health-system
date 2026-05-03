package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("care_team_member")
public class CareTeamMember {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long patientId;
    private Long doctorId;
    private String memberRoleCode;  // PRIMARY,COLLABORATOR,ON_CALL,COVERAGE,SUPERVISOR
    private String memberStatus;    // ACTIVE,INACTIVE,REMOVED
    private String accessScopeJson;
    private LocalDateTime coverageStartAt;
    private LocalDateTime coverageEndAt;
    private Long addedBy;
    private LocalDateTime addedAt;
    private LocalDateTime removedAt;
    private String removedReason;
    @Version
    private Integer version;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
