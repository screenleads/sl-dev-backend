package com.screenleads.backend.app.application.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.screenleads.backend.app.domain.model.ChatMessage;

public interface WebSocketService {
    void WSService(SimpMessagingTemplate messagingTemplate);

    void notifyFrontend(final ChatMessage message, final String roomId);
}
