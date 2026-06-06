package com.java.boilerplate.model;

import jakarta.persistence.*;
import lombok.Data;
import net.minidev.json.annotate.JsonIgnore;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "push_subscription")
public class PushSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_subscription")
    private Long id;

    @Column(name = "id_user")
    private Long idUser;

    @Column(name = "context_key", nullable = false, length = 50)
    private String contextKey;

    @Column(nullable = false)
    private String endpoint;

    @Column(nullable = false)
    private String p256dh;

    @Column(nullable = false)
    private String auth;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_user", insertable = false, updatable = false)
    private Users user;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
