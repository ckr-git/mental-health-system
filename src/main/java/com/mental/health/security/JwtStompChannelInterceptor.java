package com.mental.health.security;

import com.mental.health.common.JwtUtil;
import com.mental.health.service.UserPresenceService;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class JwtStompChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtStompChannelInterceptor.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserPresenceService userPresenceService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String bearer = accessor.getFirstNativeHeader("Authorization");
            if (bearer == null || !bearer.startsWith("Bearer ")) {
                log.warn("WebSocket CONNECT rejected: missing or invalid Authorization header");
                throw new SecurityException("WebSocket连接需要JWT认证");
            }

            String token = bearer.substring(7);
            Claims claims = jwtUtil.getClaimsFromToken(token);
            if (claims == null || jwtUtil.isTokenExpired(token)) {
                log.warn("WebSocket CONNECT rejected: invalid or expired token");
                throw new SecurityException("JWT令牌无效或已过期");
            }

            Long userId = Long.valueOf(claims.get("userId").toString());
            String username = claims.getSubject();
            String role = claims.get("role").toString();

            // Principal name uses userId string for convertAndSendToUser compatibility
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    String.valueOf(userId), null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)));
            accessor.setUser(auth);

            // Store both userId and username in session attributes
            if (accessor.getSessionAttributes() != null) {
                accessor.getSessionAttributes().put("userId", userId);
                accessor.getSessionAttributes().put("username", username);
            }

            userPresenceService.markOnline(userId, accessor.getSessionId());
            log.info("WebSocket CONNECT: user={}, userId={}, session={}", username, userId, accessor.getSessionId());
        }

        if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            Long userId = getUserIdFromSession(accessor);
            if (userId != null) {
                userPresenceService.markOffline(userId, accessor.getSessionId());
                log.info("WebSocket DISCONNECT: userId={}, session={}", userId, accessor.getSessionId());
            }
        }

        return message;
    }

    private Long getUserIdFromSession(StompHeaderAccessor accessor) {
        if (accessor.getSessionAttributes() != null) {
            Object userId = accessor.getSessionAttributes().get("userId");
            if (userId instanceof Long) {
                return (Long) userId;
            }
        }
        return null;
    }
}
