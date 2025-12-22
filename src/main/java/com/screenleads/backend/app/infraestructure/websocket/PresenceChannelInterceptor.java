package com.screenleads.backend.app.infraestructure.websocket;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.lang.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PresenceChannelInterceptor implements ChannelInterceptor {

    private static final Map<String, Set<String>> activeRooms = new ConcurrentHashMap<>();

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        String sessionId = accessor.getSessionId();

        if (accessor.getCommand() == StompCommand.SUBSCRIBE) {
            String destination = accessor.getDestination();
            if (destination != null) {
                activeRooms.computeIfAbsent(destination, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
            }
        }

        if (accessor.getCommand() == StompCommand.DISCONNECT) {
            for (Set<String> sessions : activeRooms.values()) {
                sessions.remove(sessionId);
            }
            activeRooms.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        }

        return message;
    }

    public static Map<String, Set<String>> getActiveRooms() {
        return activeRooms;
    }
}
