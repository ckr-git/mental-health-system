package com.mental.health.controller;

import com.mental.health.entity.ChatMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * 聊天控制器 - WebSocket
 */
@Controller
public class ChatController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * 发送私聊消息
     */
    @MessageMapping("/chat/private")
    public void sendPrivateMessage(@Payload ChatMessage message) {
        // 发送给指定用户
        messagingTemplate.convertAndSendToUser(
                message.getReceiverId().toString(),
                "/queue/messages",
                message
        );
        
        // 同时发送给发送者（用于消息确认）
        messagingTemplate.convertAndSendToUser(
                message.getSenderId().toString(),
                "/queue/messages",
                message
        );
    }

    /**
     * 发送广播消息
     */
    @MessageMapping("/chat/broadcast")
    @SendTo("/topic/messages")
    public ChatMessage sendBroadcastMessage(@Payload ChatMessage message) {
        return message;
    }

    /**
     * 用户上线通知
     */
    @MessageMapping("/chat/online")
    @SendTo("/topic/online")
    public ChatMessage userOnline(@Payload ChatMessage message) {
        message.setType("ONLINE");
        return message;
    }

    /**
     * 用户下线通知
     */
    @MessageMapping("/chat/offline")
    @SendTo("/topic/offline")
    public ChatMessage userOffline(@Payload ChatMessage message) {
        message.setType("OFFLINE");
        return message;
    }
}
