package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统警报实体类
 */
@Data
@TableName("system_alert")
public class SystemAlert implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 警报类型: SECURITY-安全警报, PERFORMANCE-性能警报, ERROR-错误警报, OTHER-其他
     */
    private String alertType;

    /**
     * 警报级别: INFO-信息, WARNING-警告, ERROR-错误, CRITICAL-严重
     */
    private String level;

    /**
     * 警报标题
     */
    private String title;

    /**
     * 警报内容
     */
    private String message;

    /**
     * 来源模块
     */
    private String sourceModule;

    /**
     * 详细信息（JSON格式）
     */
    private String details;

    /**
     * 处理状态: 0-未处理, 1-处理中, 2-已处理, 3-已忽略
     */
    private Integer status;

    /**
     * 处理人ID
     */
    private Long handlerId;

    /**
     * 处理备注
     */
    private String handleNote;

    /**
     * 处理时间
     */
    private LocalDateTime handleTime;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
