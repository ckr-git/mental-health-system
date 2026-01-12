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
