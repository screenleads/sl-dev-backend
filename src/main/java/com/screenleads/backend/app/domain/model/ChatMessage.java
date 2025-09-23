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
public enum MessageType { REFRESH_ADS, RESTART_APP, MAINTENANCE_MODE, NOTIFY }
private String id;
private MessageType type;
private String message;
private String senderId;
private String senderName;
private String roomId;
private Instant timestamp;
private Map<String, Object> metadata;
private boolean systemGenerated;
}