package com.screenleads.backend.app.infraestructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.messaging.simp.config.ChannelRegistration;
import lombok.RequiredArgsConstructor;

import com.screenleads.backend.app.infraestructure.websocket.PresenceChannelInterceptor;
import com.screenleads.backend.app.application.security.websocket.AuthChannelInterceptor;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfiguration implements WebSocketMessageBrokerConfigurer {

    private final PresenceChannelInterceptor presenceChannelInterceptor;
    private final AuthChannelInterceptor authChannelInterceptor;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Interceptores de presencia y autenticación JWT
        registration.interceptors(presenceChannelInterceptor, authChannelInterceptor);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/chat-socket")
                .setAllowedOriginPatterns(
                        "http://localhost:*",
                        "https://localhost",
                        "http://localhost:4200",
                        "http://localhost:8100",
                        "https://sl-device-connector.web.app",
                        "https://sl-dev-dashboard-pre-c4a3c4b00c91.herokuapp.com",
                        "https://sl-dev-dashboard-bfb13611d5d6.herokuapp.com")
                .withSockJS();
    }
}
