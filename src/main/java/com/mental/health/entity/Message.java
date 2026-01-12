package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息记录实体
 */
@Data
@TableName("message")
public class Message {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long consultationId;
    private Long senderId;
    private Long receiverId;
    private String content;
    private String messageType;
    private String fileUrl;
    private Integer isRead;
    private LocalDateTime createTime;
}
