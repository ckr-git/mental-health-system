package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统通知实体类
 */
@Data
@TableName("system_notification")
public class SystemNotification implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 通知类型: SYSTEM-系统通知, PERSONAL-个人通知, BROADCAST-广播通知
     */
    private String notificationType;

    /**
     * 目标用户ID（为空表示所有用户）
     */
    private Long targetUserId;

    /**
     * 通知标题
     */
    private String title;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 优先级: LOW-低, NORMAL-普通, HIGH-高, URGENT-紧急
     */
    private String priority;

    /**
     * 阅读状态: 0-未读, 1-已读
     */
    private Integer readStatus;

    /**
     * 发送状态: 0-待发送, 1-已发送, 2-发送失败
     */
    private Integer sendStatus;

    /**
     * 预定发送时间
     */
    private LocalDateTime scheduledTime;

    /**
     * 实际发送时间
     */
    private LocalDateTime sentTime;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
