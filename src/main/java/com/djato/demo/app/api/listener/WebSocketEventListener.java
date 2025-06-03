package com.djato.demo.app.api.listener;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {

    @EventListener
    private void handleSessionConnected(SessionConnectEvent event) {
        System.out.println("Nuevo usuario conectado al websocket");
        System.out.println(event);

    }

    @EventListener
    private void handleSessionDisconnect(SessionDisconnectEvent event) {
        System.out.println("Usuario desconectado del websocket");
    }
}