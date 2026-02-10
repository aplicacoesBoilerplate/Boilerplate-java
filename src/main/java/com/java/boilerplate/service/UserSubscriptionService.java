package com.java.boilerplate.service;

import com.java.boilerplate.enums.SubscriptionStatus;
import com.java.boilerplate.exception.ExceptionsSystem;
import com.java.boilerplate.model.UserSubscription;
import com.java.boilerplate.repository.IUserSubscriptionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserSubscriptionService {
    private final IUserSubscriptionRepository subscriptionRepository;

    public UserSubscriptionService(IUserSubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @Transactional
    public UserSubscription save(UserSubscription subscription) {
        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public void renewSubscription(Long userId, String transactionId) {
        UserSubscription subscription = this.findByUser_IdUser(userId);

        LocalDateTime now = LocalDateTime.now();

        if (subscription.getExpireAt().isBefore(now)) {
            subscription.setExpireAt(now.plusDays(30));
        } else {
            subscription.setExpireAt(subscription.getExpireAt().plusDays(30));
        }

        subscription.setLastPaymentId(transactionId);
        subscription.setStatus(SubscriptionStatus.ACTIVE);

        subscriptionRepository.save(subscription);
    }

    @Transactional(readOnly = true)
    public UserSubscription findByUser_IdUser(Long idUser) {
        return subscriptionRepository.findByUser_IdUser(idUser)
            .orElseThrow(() -> new ExceptionsSystem(
                    "Subscription not found for user",
                    HttpStatus.INTERNAL_SERVER_ERROR
            ));
    }
}
