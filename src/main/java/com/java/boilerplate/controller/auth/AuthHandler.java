package com.java.boilerplate.controller.auth;

import com.java.boilerplate.dto.auth.DTOAuth;
import com.java.boilerplate.dto.auth.DTOLoginResponse;
import com.java.boilerplate.dto.users.DTOUser;
import com.java.boilerplate.model.Users;
import com.java.boilerplate.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class AuthHandler {
    private final AuthService authService;

    public AuthHandler(AuthService authService) {
        this.authService = authService;
    }

    public ResponseEntity<DTOLoginResponse> login(DTOAuth data) {
        String token = authService.login(data);
        return ResponseEntity.ok(new DTOLoginResponse(token));
    }

    public ResponseEntity<DTOLoginResponse> register(DTOUser data) {
        Users user = data.toEntity();
        Users newUser = authService.register(user);
        return this.login(new DTOAuth(newUser.getEmail(), data.password()));
    }
}
