package com.java.boilerplate.controller.infinitePay;

import com.java.boilerplate.dto.infinitepay.DTOInfinitePayLinkResponse;
import com.java.boilerplate.model.Users;
import com.java.boilerplate.service.AuthService;
import com.java.boilerplate.service.InfinitePayService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class InfinitePayHandler {
    private final InfinitePayService infinitePayService;
    private final AuthService authService;

    public InfinitePayHandler(InfinitePayService infinitePayService, AuthService authService) {
        this.infinitePayService = infinitePayService;
        this.authService = authService;
    }

    public ResponseEntity<DTOInfinitePayLinkResponse> generateRenewalLink() {
        Users user = authService.getMeIgnoringSubscription();
        return ResponseEntity.ok(infinitePayService.createPaymentLink(user));
    }
}
