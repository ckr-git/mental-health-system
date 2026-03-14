package com.mental.health.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 聊天消息实体
 */
@Data
public class ChatMessage {
    private Long id;
    private Long senderId;
    private String senderName;
    private Long receiverId;
    private String content;
    private String type; // TEXT, IMAGE, FILE
    private LocalDateTime timestamp;
    private Integer isRead;

    public ChatMessage() {
        this.timestamp = LocalDateTime.now();
        this.isRead = 0;
    }
}
