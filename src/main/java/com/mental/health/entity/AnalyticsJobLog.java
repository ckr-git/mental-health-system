package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("analytics_job_log")
public class AnalyticsJobLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String jobCode;
    private String jobScopeCode; // PATIENT_DAILY,DOCTOR_DAILY,OUTCOME_REFRESH,FEATURE_EXTRACTION
    private String jobStatus;    // QUEUED,RUNNING,SUCCESS,FAILED,PARTIAL,CANCELLED
    private LocalDateTime targetDate;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Integer recordsProcessed;
    private String errorMessage;
    private String resultSummaryJson;
    private Long triggeredBy;
    @Version
    private Integer version;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
