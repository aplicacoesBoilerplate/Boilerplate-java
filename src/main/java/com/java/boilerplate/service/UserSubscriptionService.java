package com.java.boilerplate.service;

import com.java.boilerplate.exception.ExceptionsSystem;
import com.java.boilerplate.model.UserSubscription;
import com.java.boilerplate.repository.IUserSubscriptionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public UserSubscription update(UserSubscription subscription, Long idSubscription) {
        this.findByUser_IdUser(subscription.getUser().getIdUser());
        subscriptionRepository.findById(idSubscription).orElseThrow(() -> new ExceptionsSystem(
                "User signature not found",
                HttpStatus.NOT_FOUND
        ));
        return this.save(subscription);
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
