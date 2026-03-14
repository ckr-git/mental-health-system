package com.mental.health.service;

import com.mental.health.entity.OutboxEvent;

public interface OutboxEventHandler {
    boolean supports(String eventType);
    void handle(OutboxEvent event);
}
