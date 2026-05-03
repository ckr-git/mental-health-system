package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("notification_delivery")
public class NotificationDelivery {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long notificationId;
    private Long userId;
    private String channelCode;    // IN_APP,EMAIL,SMS,WEBSOCKET
    private String deliveryStatus; // PENDING,SENT,DELIVERED,FAILED,COALESCED
    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime failedAt;
    private String failureReason;
    private Integer retryCount;
    private String coalescedKey;
    private Integer coalescedCount;
    private Integer escalatedFlag;
    private Long escalatedToNotificationId;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
