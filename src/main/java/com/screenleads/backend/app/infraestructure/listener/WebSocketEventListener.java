package com.screenleads.backend.app.infraestructure.listener;

import com.screenleads.backend.app.domain.model.Device;
import com.screenleads.backend.app.domain.repositories.DeviceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
public class WebSocketEventListener {

    private final DeviceRepository deviceRepository;

    public WebSocketEventListener(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    @EventListener
    private void handleSessionConnected(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Long deviceId = (Long) accessor.getSessionAttributes().get("deviceId");
        
        if (deviceId != null) {
            log.info("Device {} conectado al websocket", deviceId);
            updateDeviceStatus(deviceId, true);
        } else {
            log.info("Usuario (no device) conectado al websocket");
        }
    }

    @EventListener
    private void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Long deviceId = (Long) accessor.getSessionAttributes().get("deviceId");
        
        if (deviceId != null) {
            log.info("Device {} desconectado del websocket", deviceId);
            updateDeviceStatus(deviceId, false);
        } else {
            log.info("Usuario (no device) desconectado del websocket");
        }
    }

    private void updateDeviceStatus(Long deviceId, boolean online) {
        try {
            deviceRepository.findById(deviceId).ifPresent(device -> {
                device.setOnline(online);
                if (online) {
                    device.heartbeat(); // Actualiza lastHeartbeatAt
                }
                deviceRepository.save(device);
                log.debug("Device {} marcado como online={}", deviceId, online);
            });
        } catch (Exception e) {
            log.error("Error actualizando estado del device {}: {}", deviceId, e.getMessage());
        }
    }
}