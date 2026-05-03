package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("notification_template")
public class NotificationTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String templateCode;
    private String category;
    private String defaultPriority;
    private String titleTemplate;
    private String contentTemplate;
    private String actionType;
    private String actionTemplate;
    private Integer escalationEnabled;
    private Integer escalationTimeoutMinutes;
    private String escalationTargetRole;
    private Integer mustAck;
    private Integer ackDeadlineMinutes;
    private Integer active;

    @Version
    private Integer version;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
