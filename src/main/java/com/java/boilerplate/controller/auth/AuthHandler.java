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
        return ResponseEntity.ok(authService.login(data));
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
        return ResponseEntity.ok(authService.resetPasswordWithOtp(request));
    }

    public ResponseEntity<DTOLoginResponse> refreshToken(String usernameOrEmail) {
        return ResponseEntity.ok(authService.refreshToken(usernameOrEmail));
    }

    public ResponseEntity<Void> logout() {
        authService.logout();
        return ResponseEntity.noContent().build();
    }
}
