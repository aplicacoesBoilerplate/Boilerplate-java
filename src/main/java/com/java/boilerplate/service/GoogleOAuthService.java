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
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
public class GoogleOAuthService {
    private final IUsersRepository usersRepository;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder encoder;
    private final UsersService usersService;
    private final TokensProperties properties;

    public GoogleOAuthService(IUsersRepository usersRepository, TokenService tokenService, RefreshTokenService refreshTokenService, PasswordEncoder encoder, UsersService usersService, TokensProperties properties) {
        this.usersRepository = usersRepository;
        this.tokenService = tokenService;
        this.refreshTokenService = refreshTokenService;
        this.encoder = encoder;
        this.usersService = usersService;
        this.properties = properties;
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
        newUser.setPassword(encoder.encode(data.password()));
        newUser.setPhoneNumber(data.phoneNumber());
        newUser.setUserGender(data.userGender());
        newUser.setAvatarUrl(data.pictureUrl());
        newUser.setRole(UserRoles.USER);

        newUser.setIsActive(true);
        newUser.setIsOnline(true);

        Users savedUser = usersService.saveUser(newUser);

        String jwt = tokenService.generateToken(savedUser);
        String refreshToken = refreshTokenService.createRefreshToken(savedUser);

        return new DTOLoginResponse(jwt, refreshToken);
    }
}
