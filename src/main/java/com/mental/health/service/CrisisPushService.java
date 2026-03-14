package com.mental.health.service;

import com.mental.health.dto.UserNotificationDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class CrisisPushService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void pushCrisisAlert(Long doctorId, Object alertPayload) {
        messagingTemplate.convertAndSendToUser(
                doctorId.toString(),
                "/queue/crisis-alerts",
                alertPayload
        );
    }
}
