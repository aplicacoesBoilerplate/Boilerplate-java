package com.java.boilerplate.config;

import com.java.boilerplate.service.UsersService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Optional;

@Component
public class WebSocketPresenceListener {
    private final UsersService usersService;

    public WebSocketPresenceListener(UsersService usersService) {
        this.usersService = usersService;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Optional.ofNullable(event.getUser())
                .ifPresent(user -> {
                    String username = user.getName();
                    updateUserPresence(username, true);
                });
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        Optional.ofNullable(event.getUser())
                .ifPresent(user -> {
                    String username = user.getName();
                    updateUserPresence(username, false);
                });
    }

    private void updateUserPresence(String username, boolean isOnline) {
        usersService.updatePresence(username, isOnline);
    }
}