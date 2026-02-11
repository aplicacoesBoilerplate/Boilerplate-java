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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class InfinitePayService {
    private final TokensProperties tokensProperties;
    private final UserSubscriptionService subscriptionService;
    private final UsersService usersService;

    public InfinitePayService(TokensProperties tokensProperties, UserSubscriptionService subscriptionService, UsersService usersService) {
        this.tokensProperties = tokensProperties;
        this.subscriptionService = subscriptionService;
        this.usersService = usersService;
    }

    @Transactional(readOnly = true)
    public DTOInfinitePayLinkResponse generateRenewalLink(String email) {
        try {
            Users user = usersService.findByUsernameOrEmail(email);
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

            return response.getBody();

        } catch (HttpClientErrorException e) {
            String erroGateway = e.getResponseBodyAsString();
            System.err.println("Erro vindo da InfinitePay: " + erroGateway);

            throw new ExceptionsSystem("Failed to create payment link at provider: " + erroGateway, HttpStatus.BAD_GATEWAY);

        } catch (Exception e) {
            throw new ExceptionsSystem("Payment Gateway Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
