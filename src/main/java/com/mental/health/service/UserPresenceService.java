package com.mental.health.service;

import com.mental.health.entity.UserPresence;
import com.mental.health.mapper.UserPresenceMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserPresenceService {

    private static final Logger log = LoggerFactory.getLogger(UserPresenceService.class);

    @Autowired
    private UserPresenceMapper userPresenceMapper;

    public void markOnline(Long userId, String sessionId) {
        UserPresence presence = userPresenceMapper.selectById(userId);
        if (presence == null) {
            presence = new UserPresence();
            presence.setUserId(userId);
            presence.setWebsocketSessionId(sessionId);
            presence.setClientType("WEB");
            presence.setOnlineStatus("ONLINE");
            presence.setLastSeenAt(LocalDateTime.now());
            presence.setConnectTime(LocalDateTime.now());
            userPresenceMapper.insert(presence);
        } else {
            presence.setWebsocketSessionId(sessionId);
            presence.setOnlineStatus("ONLINE");
            presence.setLastSeenAt(LocalDateTime.now());
            presence.setConnectTime(LocalDateTime.now());
            presence.setDisconnectTime(null);
            userPresenceMapper.updateById(presence);
        }
        log.info("User {} marked online, session={}", userId, sessionId);
    }

    public void markOffline(Long userId, String sessionId) {
        if (userId == null) return;

        UserPresence presence = userPresenceMapper.selectById(userId);
        if (presence != null) {
            // Only mark offline if session matches (prevents stale disconnects)
            if (sessionId == null || sessionId.equals(presence.getWebsocketSessionId())) {
                presence.setOnlineStatus("OFFLINE");
                presence.setLastSeenAt(LocalDateTime.now());
                presence.setDisconnectTime(LocalDateTime.now());
                presence.setWebsocketSessionId(null);
                userPresenceMapper.updateById(presence);
                log.info("User {} marked offline", userId);
            }
        }
    }

    public boolean isOnline(Long userId) {
        UserPresence presence = userPresenceMapper.selectById(userId);
        return presence != null && "ONLINE".equals(presence.getOnlineStatus());
    }
}
