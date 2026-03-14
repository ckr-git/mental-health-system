package com.mental.health.service;

import com.mental.health.entity.OutboxEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class OutboxRoutingRegistry {

    @Autowired(required = false)
    private List<OutboxEventHandler> handlers;

    public void route(OutboxEvent event) {
        if (handlers == null || handlers.isEmpty()) {
            log.debug("No outbox event handlers registered, skipping: {}", event.getEventType());
            return;
        }
        for (OutboxEventHandler handler : handlers) {
            if (handler.supports(event.getEventType())) {
                try {
                    handler.handle(event);
                } catch (Exception e) {
                    log.error("Handler {} failed for event {}: {}",
                            handler.getClass().getSimpleName(), event.getEventType(), e.getMessage(), e);
                    throw e;
                }
            }
        }
    }
}
