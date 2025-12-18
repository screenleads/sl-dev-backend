package com.screenleads.backend.app.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.screenleads.backend.app.application.service.WebSocketService;
import com.screenleads.backend.app.domain.model.ChatMessage;

@Slf4j
@RestController
@RequestMapping("/ws") // 👈 Alineado con /ws/status
@CrossOrigin(origins = "*") // ajusta orígenes si quieres
public class WsCommandController {

    private final WebSocketService service;

    public WsCommandController(WebSocketService service) {
        this.service = service;
    }

    // ---- Endpoint que espera tu frontend ----
    @PostMapping("/command/{roomId}")
    public ResponseEntity<String> sendCommand(
            @PathVariable String roomId,
            @RequestBody ChatMessage message) {

        if (message.getId() == null || message.getId().isBlank()) {
            message.setId(java.util.UUID.randomUUID().toString());
        }
        if (message.getTimestamp() == null) {
            message.setTimestamp(java.time.Instant.now());
        }

        log.debug("[WsCommandController] POST command room={} id={} type={} msg={}",
                roomId, message.getId(), message.getType(), message.getMessage());

        service.notifyFrontend(message, roomId);
        return ResponseEntity.accepted().body("202");
    }

    // ---- Endpoints de test que ya tenías (opcional mantenerlos) ----

    @PostMapping("/test/{roomId}")
    public ResponseEntity<String> test(@PathVariable String roomId, @RequestBody ChatMessage message) {
        log.debug("[WsCommandController] test hit room={}", roomId);
        return ResponseEntity.ok("200");
    }

    @PostMapping("/test-message/{roomId}")
    public ResponseEntity<String> sendMessageTest(
            @PathVariable String roomId,
            @RequestBody ChatMessage message) {
        log.debug("[WsCommandController] test-message room={}", roomId);
        service.notifyFrontend(message, roomId);
        return ResponseEntity.ok("200");
    }
}
