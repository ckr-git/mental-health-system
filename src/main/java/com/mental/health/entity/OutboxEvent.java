package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("outbox_event")
public class OutboxEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String aggregateType;
    private Long aggregateId;
    private String eventType;
    private String eventKey;
    private String payload;
    private String headers;
    private String status;
    private Integer retryCount;
    private Integer maxRetryCount;
    private LocalDateTime nextRetryTime;
    private String lastError;
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public static OutboxEvent pending(String aggregateType, Long aggregateId,
                                      String eventType, String eventKey, String payload) {
        OutboxEvent event = new OutboxEvent();
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setEventType(eventType);
        event.setEventKey(eventKey);
        event.setPayload(payload);
        event.setStatus("PENDING");
        event.setRetryCount(0);
        event.setMaxRetryCount(12);
        event.setNextRetryTime(LocalDateTime.now());
        return event;
    }
}
