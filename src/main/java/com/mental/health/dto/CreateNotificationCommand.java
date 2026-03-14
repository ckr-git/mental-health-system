package com.mental.health.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class CreateNotificationCommand implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userId;
    private String category;
    private String priority;
    private String title;
    private String content;
    private String actionType;
    private String actionPayload;
    private String sourceType;
    private Long sourceId;
}
