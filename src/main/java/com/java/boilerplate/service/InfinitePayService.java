package com.java.boilerplate.service;

import com.java.boilerplate.config.TokensProperties;
import com.java.boilerplate.dto.infinitepay.DTOInfinitePayLinkRequest;
import com.java.boilerplate.dto.infinitepay.DTOInfinitePayLinkResponse;
import com.java.boilerplate.exception.ExceptionsSystem;
import com.java.boilerplate.model.Users;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class InfinitePayService {
    private final TokensProperties tokensProperties;

    public InfinitePayService(TokensProperties tokensProperties) {
        this.tokensProperties = tokensProperties;
    }

    public DTOInfinitePayLinkResponse createPaymentLink(Users user) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + tokensProperties.getInfinitePayToken());

            int amountInCents = (int) (tokensProperties.getInfinitePayAmount() * 100);

            Map<String, String> metadata = new HashMap<>();
            metadata.put("userId", user.getIdUser().toString());
            metadata.put("userEmail", user.getEmail());
            metadata.put("type", "SUBSCRIPTION_RENEWAL");

            DTOInfinitePayLinkRequest requestBody = new DTOInfinitePayLinkRequest(
                    amountInCents,
                    "Subscription Renewal - " + user.getUserUsername(),
                    metadata
            );

            HttpEntity<DTOInfinitePayLinkRequest> entity = new HttpEntity<>(requestBody, headers);
            String endpoint = tokensProperties.getInfinitePayUrl() + "/transactions";

            ResponseEntity<DTOInfinitePayLinkResponse> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    DTOInfinitePayLinkResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new ExceptionsSystem("Failed to create payment link at provider", HttpStatus.BAD_GATEWAY);
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new ExceptionsSystem("Payment Gateway Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
