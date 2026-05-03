package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Data
@TableName("notification_preference")
public class NotificationPreference {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String category;    // CRISIS,APPOINTMENT,ASSESSMENT,TREATMENT,SYSTEM,CHAT
    private String channelCode; // IN_APP,EMAIL,SMS,WEBSOCKET
    private Integer enabled;
    private LocalTime quietStart;
    private LocalTime quietEnd;
    private String minPriority; // LOW,NORMAL,HIGH,URGENT
    private Integer coalesceMinutes;

    @Version
    private Integer version;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
