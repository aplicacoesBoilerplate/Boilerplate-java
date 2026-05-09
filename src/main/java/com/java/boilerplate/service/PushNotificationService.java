package com.java.boilerplate.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java.boilerplate.config.VapidProperties;
import com.java.boilerplate.dto.DTOPushNotification;
import com.java.boilerplate.exception.ExceptionsSystem;
import com.java.boilerplate.model.PushSubscription;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.apache.http.HttpResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PushNotificationService {

    private final PushSubscriptionService pushSubscriptionService;
    private final VapidProperties vapidProperties;
    private final ObjectMapper objectMapper;

    public PushNotificationService(PushSubscriptionService pushSubscriptionService, VapidProperties vapidProperties, ObjectMapper objectMapper) {
        this.pushSubscriptionService = pushSubscriptionService;
        this.vapidProperties = vapidProperties;
        this.objectMapper = objectMapper;
    }

    public void notifyUser(Long idUser, DTOPushNotification dto) {
        List<PushSubscription> subscriptions = pushSubscriptionService.findAllByIdUser(idUser);
        if (subscriptions.isEmpty()) return;
        subscriptions.forEach(subscription -> sendNotification(subscription, dto));
    }

    private void sendNotification(PushSubscription subscription, DTOPushNotification dto) {
        HttpStatus httpStatus = null;
        try {
            Subscription pushSubscription = new Subscription(
                subscription.getEndpoint(),
                new Subscription.Keys(subscription.getP256dh(), subscription.getAuth())
            );

            PushService pushService = new PushService()
                .setPublicKey(vapidProperties.getPublicKey())
                .setPrivateKey(vapidProperties.getPrivateKey())
                .setSubject(vapidProperties.getSubject());

            String payload = objectMapper.writeValueAsString(dto);

            Notification notification = new Notification(pushSubscription, payload);
            HttpResponse response = pushService.send(notification);
            httpStatus = HttpStatus.valueOf(response.getStatusLine().getStatusCode());
            handleResponse(response, subscription);

        } catch (Exception ex) {
            handleSubscriptionError(subscription, ex, httpStatus);
        }
    }

    private void handleResponse(HttpResponse response, PushSubscription subscription) {
        int statusCode = response.getStatusLine().getStatusCode();

        if (statusCode == 410 || statusCode == 404) {
            pushSubscriptionService.deleteByEndpoint(subscription.getEndpoint());
            throw new ExceptionsSystem("Erro ao enviar push para: " + subscription.getEndpoint(), HttpStatus.valueOf(statusCode));
        }
    }

    private void handleSubscriptionError(PushSubscription subscription, Exception ex, HttpStatus statusCode) {
        System.err.println("Erro ao enviar push para: " + subscription.getEndpoint() + " - " + ex.getMessage());
        throw new ExceptionsSystem(ex.getMessage(), statusCode);
    }
}
