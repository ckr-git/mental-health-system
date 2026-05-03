package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 预约实体类
 */
@Data
@TableName("appointment")
public class Appointment implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 患者ID
     */
    private Long patientId;

    /**
     * 医生ID
     */
    private Long doctorId;

    /**
     * 预约类型: CONSULTATION-咨询, THERAPY-治疗, REVIEW-复查
     */
    private String appointmentType;

    /**
     * 预约时间
     */
    private LocalDateTime appointmentTime;

    /**
     * 预约时长（分钟）
     */
    private Integer duration;

    /**
     * 预约状态: 0-待确认, 1-已确认, 2-已完成, 3-已取消, 4-已过期
     */
    private Integer status;

    /**
     * 症状描述
     */
    private String symptoms;

    /**
     * 备注
     */
    private String notes;

    /**
     * 取消原因
     */
    private String cancelReason;

    /**
     * 取消时间
     */
    private LocalDateTime cancelTime;

    /**
     * 到诊状态: PENDING,CHECKED_IN,COMPLETED,NO_SHOW,LATE_CANCEL
     */
    private String attendanceStatus;

    /**
     * 签到时间
     */
    private LocalDateTime checkInTime;

    /**
     * 取消人ID
     */
    private Long cancelledBy;

    /**
     * 改约来源预约ID
     */
    private Long rescheduledFromId;

    /**
     * 优先级: NORMAL,URGENT,FOLLOWUP,CRISIS
     */
    private String priorityCode;

    /**
     * 来源: ADMIN,PATIENT,DOCTOR,WAITLIST
     */
    private String source;

    /**
     * 候补ID
     */
    private Long waitlistId;

    /**
     * 预约类型字段兼容（数据库用appointment_type，此字段仅用于兼容旧代码）
     */
    @TableField(exist = false)
    private String type;

    /**
     * 确认时间
     */
    private LocalDateTime confirmTime;

    /**
     * 完成时间
     */
    private LocalDateTime completeTime;

    /**
     * 提醒状态: 0-未提醒, 1-已提醒
     */
    private Integer reminderSent;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
