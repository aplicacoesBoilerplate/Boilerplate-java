package com.java.boilerplate.repository;

import com.java.boilerplate.model.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IPushSubscription extends JpaRepository<PushSubscription, Long> {
    Optional<PushSubscription> findByEndpointAndContextKey(String endpoint, String contextKey);
    List<PushSubscription> findAllByIdUserAndContextKey(Long idUser, String contextKey);

    @Modifying
    void deleteAllByIdUserAndContextKey(Long idUser, String contextKey);

    @Modifying
    void deleteByEndpointAndContextKey(String endpoint, String contextKey);
}
