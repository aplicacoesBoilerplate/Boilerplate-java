package com.java.boilerplate.controller.webPush;

import com.java.boilerplate.dto.DTOPushSubscription;
import com.java.boilerplate.service.PushSubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class PushSubscriptionHandler {
    private final PushSubscriptionService service;

    public PushSubscriptionHandler(PushSubscriptionService service) {
        this.service = service;
    }

    public ResponseEntity<Void> subscribe(DTOPushSubscription dto) {
        service.saveSubscription(dto);
        return ResponseEntity.ok().build();
    }
}
