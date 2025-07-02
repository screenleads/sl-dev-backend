package com.screenleads.backend.app.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.screenleads.backend.app.application.service.WebSocketService;
import com.screenleads.backend.app.domain.model.ChatMessage;

@Controller
public class TestController {

    @Autowired
    private WebSocketService service;

    @CrossOrigin
    @PostMapping("/test/{roomId}")
    public ResponseEntity<String> test(@PathVariable String roomId, @RequestBody ChatMessage message) {
        System.out.println("Llega aqui");
        return ResponseEntity.ok("200");
    }

    @PostMapping("/test-message/{roomId}")
    public ResponseEntity<String> sendMessageTest(@PathVariable String roomId,
            @RequestBody ChatMessage message) {
        service.notifyFrontend(message, roomId);
        return ResponseEntity.ok("200");

    }

}
