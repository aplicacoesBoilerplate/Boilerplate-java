package com.java.boilerplate.controller.infinitePay;

import com.java.boilerplate.dto.infinitepay.DTOInfinitePayLinkResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/subscription")
public class InfinitePayController {
    private final InfinitePayHandler infinitePayHandler;

    public InfinitePayController(InfinitePayHandler infinitePayHandler) {
        this.infinitePayHandler = infinitePayHandler;
    }

    @PostMapping("/renew")
    public ResponseEntity<DTOInfinitePayLinkResponse> generateRenewalLink(
            @RequestParam String email
    ) { return infinitePayHandler.generateRenewalLink(email); }
}
