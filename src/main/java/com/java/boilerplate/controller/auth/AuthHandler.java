package com.java.boilerplate.controller.auth;

import com.java.boilerplate.dto.auth.DTOAuth;
import com.java.boilerplate.dto.auth.DTOLoginResponse;
import com.java.boilerplate.dto.auth.DTOOtp;
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

    public ResponseEntity<DTOUser> register(DTOUser request) {
        Users newUser = authService.register(request.toEntity());
        return ResponseEntity.ok(DTOUser.fromEntity(newUser));
    }

    public ResponseEntity<DTOLoginResponse> verifyAccount(DTOOtp request) {
        Users newUser = authService.verifyAccount(request);
        return this.login(new DTOAuth(newUser.getEmail(), request.password()));
    }

    public ResponseEntity<Void> generateOtpCode(String email) {
        authService.generateOtpCode(email);
        return ResponseEntity.ok().build();
    }

    public ResponseEntity<DTOLoginResponse> resetPasswordWithOtp(DTOOtp request) {
        return ResponseEntity.ok(new DTOLoginResponse(authService.resetPasswordWithOtp(request)));
    }
}
