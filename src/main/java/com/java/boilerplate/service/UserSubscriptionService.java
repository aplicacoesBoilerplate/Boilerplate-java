package com.java.boilerplate.service;

import com.java.boilerplate.enums.SubscriptionStatus;
import com.java.boilerplate.enums.UserRoles;
import com.java.boilerplate.exception.ExceptionsSystem;
import com.java.boilerplate.model.UserSubscription;
import com.java.boilerplate.model.Users;
import com.java.boilerplate.repository.IUserSubscriptionRepository;
import com.java.boilerplate.service.helpers.HashUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserSubscriptionService {
    private final IUserSubscriptionRepository subscriptionRepository;
    private final UsersService usersService;

    public UserSubscriptionService(IUserSubscriptionRepository subscriptionRepository, @Lazy UsersService usersService) {
        this.subscriptionRepository = subscriptionRepository;
        this.usersService = usersService;
    }

    @Transactional
    public UserSubscription save(UserSubscription subscription) {
        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public UserSubscription generateOrRecoverySubscription(Users user) {
        String currentEmailHash = HashUtil.generateSha256(user.getEmail());
        Optional<LocalDateTime> latestExpirationSubscription = this.findByEmailHash(currentEmailHash);

        UserSubscription subscription = new UserSubscription();
        subscription.setUser(user);

        if (latestExpirationSubscription.isPresent()) {
            LocalDateTime latestExpiration = latestExpirationSubscription.get();
            subscription.setExpireAt(latestExpiration);

            if (latestExpiration.isAfter(LocalDateTime.now())) {
                subscription.setStatus(SubscriptionStatus.ACTIVE);
            } else {
                subscription.setStatus(SubscriptionStatus.OVERDUE);
            }
        } else {
            subscription.setExpireAt(LocalDateTime.now().plusDays(60));
            subscription.setStatus(SubscriptionStatus.ACTIVE);
        }

        return this.save(subscription);
    }

    @Transactional
    public void renewSubscription(Long userId, String transactionId) {
        UserSubscription subscription = this.findByUser_IdUser(userId);
        LocalDateTime now = LocalDateTime.now();

        if (transactionId != null && transactionId.equals(subscription.getLastPaymentId())) return;

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
                    "Registro de contribuição não encontrado",
                    HttpStatus.INTERNAL_SERVER_ERROR
            ));
    }

    @Transactional
    public void validateSubscription(Long userId) {
        Users user = usersService.findById(userId);
        if (!user.getRole().equals(UserRoles.ADMIN)) {
            UserSubscription subscription = this.findByUser_IdUser(userId);

            if (!subscription.isValid()) {
                if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
                    subscription.setStatus(SubscriptionStatus.OVERDUE);
                    this.save(subscription);
                }
                throw new ExceptionsSystem(
                        "Solicitamos a sua contribuição para mantermos o app",
                        HttpStatus.PAYMENT_REQUIRED
                );
            }
        }
    }

    @Transactional
    public Optional<LocalDateTime> findByEmailHash(String emailHash) {
        return subscriptionRepository.findLatestExpirationByEmailHash(emailHash);
    }
}
