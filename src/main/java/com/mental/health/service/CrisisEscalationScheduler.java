package com.mental.health.service;

import com.mental.health.dto.CreateNotificationCommand;
import com.mental.health.entity.CrisisAlert;
import com.mental.health.entity.CrisisAlertAction;
import com.mental.health.mapper.CrisisAlertActionMapper;
import com.mental.health.mapper.CrisisAlertMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class CrisisEscalationScheduler {

    @Autowired
    private CrisisAlertMapper crisisAlertMapper;

    @Autowired
    private CrisisAlertActionMapper crisisAlertActionMapper;

    @Autowired
    private UserNotificationService userNotificationService;

    @Autowired
    private CrisisPushService crisisPushService;

    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void escalateOverdueAlerts() {
        List<CrisisAlert> overdue = crisisAlertMapper.findOverdueOpenAlerts(LocalDateTime.now());
        for (CrisisAlert alert : overdue) {
            alert.setAlertStatus("ESCALATED");
            crisisAlertMapper.updateById(alert);

            CrisisAlertAction action = new CrisisAlertAction();
            action.setAlertId(alert.getId());
            action.setActionType("ESCALATED");
            action.setOperatorRole("SYSTEM");
            action.setActionNote("SLA超时自动升级");
            action.setActionTime(LocalDateTime.now());
            crisisAlertActionMapper.insert(action);

            if (alert.getDoctorId() != null) {
                CreateNotificationCommand cmd = new CreateNotificationCommand();
                cmd.setUserId(alert.getDoctorId());
                cmd.setCategory("CRISIS");
                cmd.setPriority("URGENT");
                cmd.setTitle("告警已升级：" + alert.getAlertLevel());
                cmd.setContent("告警 #" + alert.getId() + " 已因SLA超时自动升级，请立即处理");
                cmd.setSourceType("CRISIS_ALERT");
                cmd.setSourceId(alert.getId());
                userNotificationService.createNotification(cmd);

                crisisPushService.pushCrisisAlert(alert.getDoctorId(),
                        java.util.Map.of("alertId", alert.getId(), "action", "ESCALATED",
                                "alertLevel", alert.getAlertLevel()));
            }

            log.warn("Crisis alert {} escalated due to SLA timeout", alert.getId());
        }
    }
}
