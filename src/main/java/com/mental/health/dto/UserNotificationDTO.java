package com.mental.health.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class UserNotificationDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String category;
    private String priority;
    private String title;
    private String content;
    private String actionType;
    private String actionPayload;
    private String sourceType;
    private Long sourceId;
    private Integer readStatus;
    private LocalDateTime createTime;
}
