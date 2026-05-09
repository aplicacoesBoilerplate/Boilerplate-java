package com.java.boilerplate.controller.webPush;

import com.java.boilerplate.dto.DTOPushSubscription;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/push")
public class PushSubscriptionController {
    private final PushSubscriptionHandler handler;

    public PushSubscriptionController(PushSubscriptionHandler handler) {
        this.handler = handler;
    }

    @PostMapping("/subscribe")
    public ResponseEntity<Void> subscribe(
            @RequestBody DTOPushSubscription dto
    ) { return handler.subscribe(dto); }
}
