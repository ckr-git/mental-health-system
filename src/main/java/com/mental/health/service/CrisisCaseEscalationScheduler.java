package com.mental.health.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 危机案例超时升级调度器
 * 每分钟检查SLA超时的案例并自动升级
 */
@Component
public class CrisisCaseEscalationScheduler {

    private static final Logger log = LoggerFactory.getLogger(CrisisCaseEscalationScheduler.class);

    @Autowired
    private CrisisCaseService crisisCaseService;

    @Scheduled(fixedDelay = 60_000) // 每分钟检查
    public void checkOverdueCases() {
        try {
            crisisCaseService.processOverdueCases();
        } catch (Exception e) {
            log.error("危机案例超时检查失败: {}", e.getMessage(), e);
        }
    }
}
