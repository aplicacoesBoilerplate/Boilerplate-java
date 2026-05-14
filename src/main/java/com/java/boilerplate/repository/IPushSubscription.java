package com.java.boilerplate.repository;

import com.java.boilerplate.model.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IPushSubscription extends JpaRepository<PushSubscription, Long> {
    Optional<PushSubscription> findByEndpoint(String endpoint);
    List<PushSubscription> findAllByIdUser(Long idUser);

    @Modifying
    void deleteAllByIdUser(Long idUser);

    @Modifying
    void deleteByEndpoint(String endpoint);
}
