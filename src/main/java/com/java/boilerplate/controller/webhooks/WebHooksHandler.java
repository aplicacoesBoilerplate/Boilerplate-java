package com.java.boilerplate.controller.webhooks;

import com.java.boilerplate.dto.infinitepay.DTOInfinitePayWebhook;
import com.java.boilerplate.service.SocketService;
import com.java.boilerplate.service.UserSubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class WebHooksHandler {
    private final UserSubscriptionService subscriptionService;
    private final SocketService socketService;

    public WebHooksHandler(UserSubscriptionService subscriptionService, SocketService socketService) {
        this.subscriptionService = subscriptionService;
        this.socketService = socketService;
    }

    public ResponseEntity<Void> infinitePay(DTOInfinitePayWebhook payload) {
        if (payload.data() != null && "approved".equalsIgnoreCase(payload.data().status())) {
            String userIdStr = payload.data().metadata().get("userId");

            if (userIdStr != null) {
                Long userId = Long.parseLong(userIdStr);
                String transactionId = payload.data().id();

                // 1. Atualiza o banco
                subscriptionService.renewSubscription(userId, transactionId);

                // 2. Avisa o Front em tempo real
                socketService.notifyPaymentSuccess(userId);
            }
        }

        return ResponseEntity.ok().build();
    }
}
