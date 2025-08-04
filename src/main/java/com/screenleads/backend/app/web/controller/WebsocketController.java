package com.screenleads.backend.app.web.controller;

import java.time.Instant;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

import com.screenleads.backend.app.domain.model.ChatMessage;

@Controller
public class WebsocketController {
    @CrossOrigin
    @MessageMapping("/chat/{roomId}")
    @SendTo("/topic/{roomId}")
    public ChatMessage chat(@DestinationVariable String roomId, ChatMessage message) {
        message.setRoomId(roomId);
        message.setTimestamp(Instant.now());
        return message;
    }
}
