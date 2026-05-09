package com.java.boilerplate.service;

import com.java.boilerplate.dto.DTOPushSubscription;
import com.java.boilerplate.model.PushSubscription;
import com.java.boilerplate.repository.IPushSubscription;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PushSubscriptionService {
    private final IPushSubscription repository;
    private final AuthService authService;

    public PushSubscriptionService(IPushSubscription pushSubscription, AuthService authService) {
        this.repository = pushSubscription;
        this.authService = authService;
    }

    public void saveSubscription(DTOPushSubscription dto) {
        Optional<PushSubscription> optional = this.findByEndpoint(dto.getEndpoint());
        PushSubscription newSub = optional.orElseGet(PushSubscription::new);

        newSub.setIdUser(authService.getMeIgnoringSubscription().getIdUser());
        newSub.setEndpoint(dto.getEndpoint());
        newSub.setP256dh(dto.getKeys().getP256dh());
        newSub.setAuth(dto.getKeys().getAuth());

        repository.save(newSub);
    }

    public Optional<PushSubscription> findByEndpoint(String endpoint) {
        return repository.findByEndpoint(endpoint);
    }

    public List<PushSubscription> findAllByIdUser(Long idUser) {
        return repository.findAllByIdUser(idUser);
    }

    public void deleteAllByIdUser(Long idUser) {
        repository.deleteAllByIdUser(idUser);
    }

    public void deleteByEndpoint(String endpoint) {
        repository.deleteByEndpoint(endpoint);
    }
}
