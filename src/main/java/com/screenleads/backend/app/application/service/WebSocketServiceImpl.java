package com.screenleads.backend.app.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.screenleads.backend.app.domain.model.ChatMessage;

@Slf4j
@Service
public class WebSocketServiceImpl implements WebSocketService {

    private SimpMessagingTemplate messagingTemplate = null;

    @Autowired
    public void wsService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void notifyFrontend(final ChatMessage message, final String roomId) {

        log.debug("Enviando mensaje a /topic/{}: {}", roomId, message.getMessage());

        messagingTemplate.convertAndSend("/topic/" + roomId, message);
    }

}