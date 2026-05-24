package com.java.boilerplate.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java.boilerplate.config.VapidProperties;
import com.java.boilerplate.dto.DTOPushNotification;
import com.java.boilerplate.model.PushSubscription;
import jakarta.annotation.PostConstruct;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import nl.martijndwars.webpush.Urgency;
import nl.martijndwars.webpush.Utils;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import nl.martijndwars.webpush.Urgency;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.List;

@Service
public class PushNotificationService {

    private final PushSubscriptionService pushSubscriptionService;
    private final VapidProperties vapidProperties;
    private final ObjectMapper objectMapper;
    private PushService pushService;

    public PushNotificationService(PushSubscriptionService pushSubscriptionService, VapidProperties vapidProperties, ObjectMapper objectMapper) {
        this.pushSubscriptionService = pushSubscriptionService;
        this.vapidProperties = vapidProperties;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        try {
            if (Security.getProvider("BC") == null) {
                Security.addProvider(new BouncyCastleProvider());
            }

            this.pushService = new PushService()
                .setPublicKey(vapidProperties.getPublicKey())
                .setPrivateKey(vapidProperties.getPrivateKey())
                .setSubject(vapidProperties.getSubject());
        } catch (Exception e) {
            System.err.println("Erro crítico ao inicializar chaves VAPID do Web Push: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public void notifyUser(Long idUser, DTOPushNotification dto) {
        List<PushSubscription> subscriptions = pushSubscriptionService.findAllByIdUser(idUser);
        if (subscriptions.isEmpty()) return;
        subscriptions.forEach(subscription -> sendNotification(subscription, dto));
    }

    private void sendNotification(PushSubscription subscription, DTOPushNotification dto) {
        HttpStatus httpStatus = null;
        try {
            String payload = objectMapper.writeValueAsString(dto);
            ECPublicKey publicKey = (ECPublicKey) Utils.loadPublicKey(subscription.getP256dh());

            byte[] authBytes = Base64.getUrlDecoder().decode(subscription.getAuth());

            Notification notification = new Notification(
                subscription.getEndpoint(),
                publicKey,
                authBytes,
                payload.getBytes(StandardCharsets.UTF_8),
                24 * 60 * 60,
                Urgency.HIGH,
                null
            );

            HttpResponse response = this.pushService.send(notification);

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
        }
    }

    private void handleSubscriptionError(PushSubscription subscription, Exception ex, HttpStatus status) {
        System.err.println("Erro " + status + " ao enviar push para: " + subscription.getEndpoint() + " -> " + ex.getMessage());
    }
}
