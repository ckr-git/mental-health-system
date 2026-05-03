package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("referral_handoff")
public class ReferralHandoff {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long referralCaseId;
    private Long createdBy;
    private String handoffStatus;  // DRAFT,READY,ACKNOWLEDGED
    private String recentAssessmentsJson;
    private String riskHistoryJson;
    private String currentPlanSummary;
    private String recentSessionsSummary;
    private String medicationSummary;
    private String pendingTasksJson;
    private String specialNotes;
    private Long acknowledgedBy;
    private LocalDateTime acknowledgedAt;
    @Version
    private Integer version;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
