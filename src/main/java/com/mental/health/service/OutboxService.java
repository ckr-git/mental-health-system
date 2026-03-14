package com.mental.health.service;

import com.mental.health.entity.OutboxEvent;
import com.mental.health.mapper.OutboxEventMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OutboxService {

    private static final Logger log = LoggerFactory.getLogger(OutboxService.class);

    @Autowired
    private OutboxEventMapper outboxEventMapper;

    @Autowired
    private OutboxRoutingRegistry routingRegistry;

    @Transactional
    public void append(String aggregateType, Long aggregateId,
                       String eventType, String eventKey, String payload) {
        if (outboxEventMapper.existsByEventKey(eventKey)) {
            log.debug("Outbox event already exists, skipping: {}", eventKey);
            return;
        }
        OutboxEvent event = OutboxEvent.pending(aggregateType, aggregateId,
                eventType, eventKey, payload);
        outboxEventMapper.insert(event);
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void dispatchBatch() {
        List<OutboxEvent> batch = outboxEventMapper.lockNextBatch(100, LocalDateTime.now());
        if (batch.isEmpty()) {
            return;
        }

        log.debug("Dispatching {} outbox events", batch.size());
        for (OutboxEvent event : batch) {
            try {
                route(event);
                outboxEventMapper.markSent(event.getId());
            } catch (Exception ex) {
                if (event.getRetryCount() >= event.getMaxRetryCount()) {
                    log.error("Outbox event {} exceeded max retries, marking dead", event.getId(), ex);
                    outboxEventMapper.markDead(event.getId(), truncate(ex.getMessage(), 490));
                } else {
                    LocalDateTime nextRetry = computeNextRetry(event.getRetryCount());
                    log.warn("Outbox event {} failed, scheduling retry at {}", event.getId(), nextRetry, ex);
                    outboxEventMapper.scheduleRetry(event.getId(), nextRetry, truncate(ex.getMessage(), 490));
                }
            }
        }
    }

    private void route(OutboxEvent event) {
        log.info("Routing outbox event: type={}, key={}", event.getEventType(), event.getEventKey());
        routingRegistry.route(event);
    }

    private LocalDateTime computeNextRetry(int currentRetryCount) {
        // Exponential backoff: 10s, 30s, 1m, 2m, 5m, 10m, 30m, 1h, 2h, 4h, 8h, 12h
        long[] delaySeconds = {10, 30, 60, 120, 300, 600, 1800, 3600, 7200, 14400, 28800, 43200};
        int index = Math.min(currentRetryCount, delaySeconds.length - 1);
        return LocalDateTime.now().plusSeconds(delaySeconds[index]);
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() > maxLen ? s.substring(0, maxLen) : s;
    }
}
