package com.java.boilerplate.service;

import com.java.boilerplate.dto.infinitepay.DTOInfinitePayWebhook;
import com.java.boilerplate.model.Users;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class WebhooksService {
    private final UserSubscriptionService subscriptionService;
    private final SocketService socketService;
    private final UsersService usersService;

    public WebhooksService(UserSubscriptionService subscriptionService, SocketService socketService, UsersService usersService) {
        this.subscriptionService = subscriptionService;
        this.socketService = socketService;
        this.usersService = usersService;
    }

    @Async
    public void infinitePayResponsePayment(DTOInfinitePayWebhook payload) {
        try {
            if (payload != null && payload.order_nsu() != null) {
                String orderNsu = payload.order_nsu();
                String username = orderNsu.split("user:")[1].split("-")[0];
                Users user = usersService.findByUsernameOrEmail(username);

                subscriptionService.renewSubscription(user.getIdUser(), payload.transaction_nsu());
                socketService.notifyPaymentSuccess(user.getIdUser());
            }
        } catch (Exception e) {
            System.err.println("Erro ao processar Webhook assíncrono: " + e.getMessage());
        }
    }
}
