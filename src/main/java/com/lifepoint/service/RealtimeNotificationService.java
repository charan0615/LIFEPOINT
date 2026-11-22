package com.lifepoint.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RealtimeNotificationService {
    private final SimpMessagingTemplate messagingTemplate;

    public RealtimeNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastDashboardUpdate(String userEmail, String eventType, Object payload) {
        try {
            Map<String, Object> message = Map.of(
                "type", eventType,
                "email", userEmail,
                "timestamp", System.currentTimeMillis(),
                "payload", payload != null ? payload : Map.of()
            );
            messagingTemplate.convertAndSend("/topic/dashboard/" + userEmail, message);
            messagingTemplate.convertAndSend("/topic/dashboard", message);
        } catch (Exception ignored) {
        }
    }
}
