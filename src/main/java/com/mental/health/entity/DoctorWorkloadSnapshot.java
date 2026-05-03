package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("doctor_workload_snapshot")
public class DoctorWorkloadSnapshot {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long doctorId;
    private LocalDateTime snapshotAt;
    private String snapshotStatus;
    private Integer scheduledAppointments;
    private Integer completedAppointments;
    private Integer noShowCount;
    private Integer crisisOpenCount;
    private Integer crisisOverdueCount;
    private Integer activePlans;
    private Integer activeReferrals;
    private Integer pendingReviews;
    private Integer pendingHandoffTasks;
    private Integer waitlistPendingCount;
    private BigDecimal avgResponseHours;
    @Version
    private Integer version;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
