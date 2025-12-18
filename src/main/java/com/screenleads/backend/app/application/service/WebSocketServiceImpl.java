package com.screenleads.backend.app.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.screenleads.backend.app.domain.model.ChatMessage;

@Service
public class WebSocketServiceImpl implements WebSocketService {

    private SimpMessagingTemplate messagingTemplate = null;

    @Autowired
    public void WSService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void notifyFrontend(final ChatMessage message, final String roomId) {

        System.out.println("Llega aqui" + roomId + message.getMessage());

        messagingTemplate.convertAndSend("/topic/" + roomId, message);
    }

}