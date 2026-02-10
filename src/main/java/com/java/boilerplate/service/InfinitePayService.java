package com.java.boilerplate.service;

import com.java.boilerplate.config.TokensProperties;
import com.java.boilerplate.dto.infinitepay.DTOInfinitePayCustomerLinkRequest;
import com.java.boilerplate.dto.infinitepay.DTOInfinitePayItemsLinkRequest;
import com.java.boilerplate.dto.infinitepay.DTOInfinitePayLinkRequest;
import com.java.boilerplate.dto.infinitepay.DTOInfinitePayLinkResponse;
import com.java.boilerplate.exception.ExceptionsSystem;
import com.java.boilerplate.model.UserSubscription;
import com.java.boilerplate.model.Users;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class InfinitePayService {
    private final TokensProperties tokensProperties;
    private final UserSubscriptionService subscriptionService;

    public InfinitePayService(TokensProperties tokensProperties, UserSubscriptionService subscriptionService) {
        this.tokensProperties = tokensProperties;
        this.subscriptionService = subscriptionService;
    }

    public DTOInfinitePayLinkResponse createPaymentLink(Users user) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            List<DTOInfinitePayItemsLinkRequest> subscriptionItem = new ArrayList<>();

            subscriptionItem.add(new DTOInfinitePayItemsLinkRequest(
                    1,
                    1000,
                    "Subscription Renewal - " + user.getUserUsername()
            ));

            DTOInfinitePayCustomerLinkRequest customer = new DTOInfinitePayCustomerLinkRequest(
                    user.getUserUsername(),
                    user.getEmail(),
                    user.getPhoneNumber()
            );

            UserSubscription subscriptionUser = subscriptionService.findByUser_IdUser(user.getIdUser());
            String order_nsu = String.format(
                    "sub:%d-user:%s-expired:%s",
                    subscriptionUser.getId(),
                    user.getUsername(),
                    subscriptionUser.getExpireAt().toString()
            );

            DTOInfinitePayLinkRequest requestBody = new DTOInfinitePayLinkRequest(
                    tokensProperties.getInfinitePayHandle(),
                    subscriptionItem,
                    order_nsu,
                    tokensProperties.getInfinitePayRedirectUrl(),
                    tokensProperties.getInfinitePayWebhookUrl(),
                    customer

            );

            HttpEntity<DTOInfinitePayLinkRequest> entity = new HttpEntity<>(requestBody, headers);
            String endpoint = tokensProperties.getInfinitePayEndpoint() + "/links";

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
