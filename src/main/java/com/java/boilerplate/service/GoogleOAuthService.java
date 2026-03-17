package com.java.boilerplate.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.java.boilerplate.config.TokensProperties;
import com.java.boilerplate.config.security.TokenService;
import com.java.boilerplate.dto.auth.DTOLoginResponse;
import com.java.boilerplate.dto.googleOAuth.DTOGoogleAuthResponse;
import com.java.boilerplate.dto.googleOAuth.DTOGoogleComplete;
import com.java.boilerplate.enums.UserRoles;
import com.java.boilerplate.exception.ExceptionsSystem;
import com.java.boilerplate.model.Users;
import com.java.boilerplate.repository.IUsersRepository;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
public class GoogleOAuthService {
    private final IUsersRepository usersRepository;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;
    private final UsersService usersService;
    private final TokensProperties properties;
    private final UserSubscriptionService userSubscriptionService;

    public GoogleOAuthService(IUsersRepository usersRepository, TokenService tokenService, RefreshTokenService refreshTokenService, UsersService usersService, TokensProperties properties, UserSubscriptionService userSubscriptionService) {
        this.usersRepository = usersRepository;
        this.tokenService = tokenService;
        this.refreshTokenService = refreshTokenService;
        this.usersService = usersService;
        this.properties = properties;
        this.userSubscriptionService = userSubscriptionService;
    }

    @Transactional
    public DTOGoogleAuthResponse processGoogleToken(String credential) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(properties.getGoogleClientId()))
                    .build();

            GoogleIdToken idToken = verifier.verify(credential);
            if (idToken == null) {
                throw new ExceptionsSystem("Token do Google inválido", HttpStatus.UNAUTHORIZED);
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String pictureUrl = (String) payload.get("picture");

            Users user = usersRepository.findByUsernameOrEmail(email);

            if (user != null) {
                user.setIsOnline(true);
                usersRepository.save(user);
                String jwt = tokenService.generateToken(user);
                String refresh = refreshTokenService.createRefreshToken(user);
                return new DTOGoogleAuthResponse(false, jwt, refresh, null, null, null);
            } else {
                String tempToken = tokenService.generateTempRegistrationToken(email);
                return new DTOGoogleAuthResponse(true, tempToken, null, name, email, pictureUrl);
            }

        } catch (Exception e) {
            throw new ExceptionsSystem("Erro ao autenticar com o Google: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public DTOLoginResponse completeRegistration(DTOGoogleComplete data) {
        String email = tokenService.validateTempRegistrationToken(data.tempToken());
        if (email == null) {
            throw new ExceptionsSystem("Sessão de cadastro expirada ou inválida.", HttpStatus.UNAUTHORIZED);
        }

        Users newUser = new Users();
        newUser.setEmail(email);
        newUser.setFullName(data.name());
        newUser.setUserUsername(data.username());
        newUser.setPassword(data.password());
        newUser.setPhoneNumber(data.phoneNumber());
        newUser.setUserGender(data.userGender());
        newUser.setAvatarUrl(null);
        newUser.setRole(UserRoles.USER);
        newUser.setIsActive(true);
        newUser.setIsOnline(true);

        double defaultLng = -42.9408;
        double defaultLat = -21.1214;

        GeometryFactory factory = new GeometryFactory(new PrecisionModel(), 4326);
        Point defaultLocation = factory.createPoint(new Coordinate(defaultLng, defaultLat));

        newUser.setLocation(defaultLocation);

        Users savedUser = usersService.saveUser(newUser);

        if (!savedUser.getRole().equals(UserRoles.ADMIN)) {
            userSubscriptionService.generateOrRecoverySubscription(savedUser);
        }

        String jwt = tokenService.generateToken(savedUser);
        String refreshToken = refreshTokenService.createRefreshToken(savedUser);

        return new DTOLoginResponse(jwt, refreshToken);
    }
}
