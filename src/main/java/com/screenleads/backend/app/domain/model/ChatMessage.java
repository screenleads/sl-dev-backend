package com.screenleads.backend.app.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    public enum MessageType {
        REFRESH_ADS, RESTART_APP, MAINTENANCE_MODE, NOTIFY
    }
    private MessageType type;
    private String message;        // Texto plano o descripción
    private String senderId;       // ID del usuario que lo envió
    private String senderName;     // Nombre visible del usuario (opcional)
    private String roomId;         // ID de la sala de chat
    private Instant timestamp;     // Fecha y hora del mensaje (en UTC)
    private Map<String, Object> metadata; // Datos extra (JSON dinámico)
    private boolean systemGenerated;      // ¿Es un mensaje generado por el sistema?

}
