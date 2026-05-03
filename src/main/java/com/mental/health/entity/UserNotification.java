package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("user_notification")
public class UserNotification implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String category;
    private String priority;
    private String title;
    private String content;
    private String actionType;
    private String actionPayload;
    private String sourceType;
    private Long sourceId;
    private String templateCode;
    private Integer mustAck;
    private LocalDateTime ackDeadline;
    private LocalDateTime ackedAt;
    private String deliveryStatus;
    private String coalescedKey;
    private Integer coalescedCount;
    private String escalationStatus;
    @Version
    private Integer version;
    private Integer readStatus;
    private LocalDateTime readTime;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
