package com.screenleads.backend.app.web.controller;

import com.screenleads.backend.app.infraestructure.websocket.PresenceChannelInterceptor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/ws")
@CrossOrigin(origins = "*")
public class WebSocketStatusController {

    @GetMapping("/status")
    public Map<String, Set<String>> getActiveRooms() {
        return PresenceChannelInterceptor.getActiveRooms();
    }
}
