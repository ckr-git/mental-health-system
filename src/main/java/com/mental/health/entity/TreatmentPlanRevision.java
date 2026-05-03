package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("treatment_plan_revision")
public class TreatmentPlanRevision {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long planId;
    private Integer fromVersion;
    private Integer toVersion;
    private String revisionStatus; // PROPOSED,APPROVED,APPLIED,REJECTED
    private Long proposedBy;
    private LocalDateTime proposedAt;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private LocalDateTime appliedAt;
    private String changeSummary;
    private String changeDetailJson;
    private String revisionReason;
    private Long reviewId;
    @Version
    private Integer version;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
