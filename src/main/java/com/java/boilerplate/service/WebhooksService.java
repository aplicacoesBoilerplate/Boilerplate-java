package com.java.boilerplate.service;

import com.java.boilerplate.dto.infinitepay.DTOInfinitePayWebhook;
import com.java.boilerplate.model.UserSubscription;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class WebhooksService {
    private final UserSubscriptionService subscriptionService;
    private final SocketService socketService;

    public WebhooksService(UserSubscriptionService subscriptionService, SocketService socketService) {
        this.subscriptionService = subscriptionService;
        this.socketService = socketService;
    }

    @Async
    public void infinitePayResponsePayment(DTOInfinitePayWebhook payload) {
        try {
            if (payload != null && payload.order_nsu() != null) {
                String orderNsu = payload.order_nsu();
                String contextKey = orderNsu.split("ctx:")[1].split("-")[0];
                Long subscriptionId = Long.parseLong(orderNsu.split("sub:")[1].split("-")[0]);

                UserSubscription subscription = subscriptionService.renewSubscriptionById(subscriptionId, contextKey, payload.transaction_nsu());
                socketService.notifyPaymentSuccess(contextKey, subscription.getUser().getIdUser());
            }
        } catch (Exception e) {
            System.err.println("Erro ao processar Webhook assíncrono: " + e.getMessage());
        }
    }
}
