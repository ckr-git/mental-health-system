package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("user_presence")
public class UserPresence implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "user_id", type = IdType.INPUT)
    private Long userId;

    private String websocketSessionId;
    private String clientType;
    private String onlineStatus;
    private LocalDateTime lastSeenAt;
    private LocalDateTime connectTime;
    private LocalDateTime disconnectTime;
}
