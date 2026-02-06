package com.java.boilerplate.controller.auth;

import com.java.boilerplate.config.security.TokenService;
import com.java.boilerplate.dto.auth.DTOAuth;
import com.java.boilerplate.dto.auth.DTOLoginResponse;
import com.java.boilerplate.dto.users.DTOUser;
import com.java.boilerplate.model.Users;
import com.java.boilerplate.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class AuthHandler {
    private final AuthenticationManager manager;
    private final TokenService tokenService;
    private final AuthService authService;

    public AuthHandler(AuthenticationManager manager, TokenService tokenService, AuthService authService) {
        this.manager = manager;
        this.tokenService = tokenService;
        this.authService = authService;
    }

    public ResponseEntity<DTOLoginResponse> login(DTOAuth data) {
        var authToken = new UsernamePasswordAuthenticationToken(data.usernameOrEmail(), data.password());
        var authentication = manager.authenticate(authToken);

        var token = tokenService.generateToken((Users) authentication.getPrincipal());
        var userResponse = DTOUser.fromEntity((Users) authentication.getPrincipal());

        return ResponseEntity.ok(new DTOLoginResponse(token, userResponse));
    }

    public ResponseEntity<DTOLoginResponse> register(DTOUser data) {
        Users user = DTOUser.toEntity(data);
        Users newUser = authService.register(user);
        return this.login(new DTOAuth(newUser.getEmail(), data.password()));
    }

    public ResponseEntity<DTOUser> getMe(String token) {
        Users user = authService.getMe(token);
        return ResponseEntity.ok(DTOUser.fromEntity(user));
    }
}
