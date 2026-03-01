package com.java.boilerplate.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.java.boilerplate.enums.SubscriptionStatus;
import com.java.boilerplate.modelQueryJPA.UserSubscriptionQueriesJPA;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_subscriptions")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class UserSubscription extends UserSubscriptionQueriesJPA {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_user", nullable = false, unique = true)
    private Users user;

    @Column(name = "expire_at", nullable = false)
    private LocalDateTime expireAt;

    @Column(name = "last_payment_id")
    private String lastPaymentId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status;

    public boolean isValid() {
        return expireAt.isAfter(LocalDateTime.now());
    }
}
