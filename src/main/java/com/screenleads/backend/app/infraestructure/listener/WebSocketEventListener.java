package com.screenleads.backend.app.infraestructure.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
public class WebSocketEventListener {

    @EventListener
    private void handleSessionConnected(SessionConnectEvent event) {
        log.info("Nuevo usuario conectado al websocket");

    }

    @EventListener
    private void handleSessionDisconnect(SessionDisconnectEvent event) {
        log.info("Usuario desconectado del websocket");
    }
}