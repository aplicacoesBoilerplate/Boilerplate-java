package com.java.boilerplate.service;

import com.java.boilerplate.dto.chatMessages.DTOChatMessages;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class SocketService {
    private final SimpMessagingTemplate template;

    public SocketService(SimpMessagingTemplate template) {
        this.template = template;
    }

    public void notifyPaymentSuccess(String contextKey, Long userId) {
        String destination = "/topic/" + contextKey + "/payment/" + userId;
        template.convertAndSend(destination, "PAYMENT_APPROVED");
    }

    public void notifyNewMessage(String usernameReceiver, DTOChatMessages messagePayload) {
        template.convertAndSendToUser(
                usernameReceiver,
                "/topic/messages",
                messagePayload
        );
    }
}
