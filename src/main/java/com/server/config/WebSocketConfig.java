package com.server.config;

import com.server.message.service.ChatWebSocketHandler;
import com.server.push.handle.NotificationHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;


@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Autowired private HandshakeInterceptor handshakeInterceptor;
    @Autowired private NotificationHandler notificationHandler;
    @Autowired private ChatWebSocketHandler chatWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notificationHandler,"/api/ws/push")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins("*");

        registry.addHandler(chatWebSocketHandler,"/api/ws/chat")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins("*");
    }
}
