package com.java.boilerplate.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class SocketService {
    private final SimpMessagingTemplate template;

    public SocketService(SimpMessagingTemplate template) {
        this.template = template;
    }

    public void notifyPaymentSuccess(Long userId) {
        String destination = "/topic/payment/" + userId;
        template.convertAndSend(destination, "PAYMENT_APPROVED");
    }
}