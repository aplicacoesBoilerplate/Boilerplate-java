package com.java.boilerplate.service;

import com.java.boilerplate.config.TokensProperties;
import com.java.boilerplate.dto.infinitepay.*;
import com.java.boilerplate.dto.users.DTOLocation;
import com.java.boilerplate.exception.ExceptionsSystem;
import com.java.boilerplate.model.UserSubscription;
import com.java.boilerplate.model.Users;
import com.java.boilerplate.service.context.AppContextService;
import com.java.boilerplate.service.helpers.GoogleServices;
import org.locationtech.jts.geom.Point;
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
    private final GoogleServices googleServices;
    private final AppContextService appContextService;

    public InfinitePayService(TokensProperties tokensProperties, UserSubscriptionService subscriptionService, UsersService usersService, GoogleServices googleServices, AppContextService appContextService) {
        this.tokensProperties = tokensProperties;
        this.subscriptionService = subscriptionService;
        this.usersService = usersService;
        this.googleServices = googleServices;
        this.appContextService = appContextService;
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
                    "Contribuição - " + user.getUserUsername()
            ));

            DTOInfinitePayCustomerLinkRequest customer = new DTOInfinitePayCustomerLinkRequest(
                    user.getUserUsername(),
                    user.getEmail(),
                    user.getPhoneNumber()
            );

            UserSubscription subscriptionUser = subscriptionService.findByUser_IdUser(user.getIdUser());
            String order_nsu = String.format(
                    "ctx:%s-sub:%d-user:%s-expired:%s",
                    appContextService.getCurrentKey(),
                    subscriptionUser.getId(),
                    user.getUserUsername(),
                    subscriptionUser.getExpireAt().toString()
            );

            Point p = user.getLocation();
            DTOLocation userLocation = new DTOLocation(p.getY(), p.getX());

            DTOInfinitePayAddressLinkRequest address = this.googleServices.fetchAddressFromGoogle(
                    userLocation, restTemplate
            );

            DTOInfinitePayLinkRequest requestBody = new DTOInfinitePayLinkRequest(
                    tokensProperties.getInfinitePayHandle(),
                    subscriptionItem,
                    order_nsu,
                    tokensProperties.getInfinitePayRedirectUrl(),
                    tokensProperties.getInfinitePayWebhookUrl(),
                    user.getPhoneNumber().isEmpty() ? null : customer,
                    address
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
            throw new ExceptionsSystem("Erro no provedor ao criar link de pagamento: " + erroGateway, HttpStatus.BAD_GATEWAY);

        } catch (Exception e) {
            throw new ExceptionsSystem("Erro ao gerar link de pagamento: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
