package com.java.boilerplate.controller.auth;

import com.fasterxml.jackson.annotation.JsonView;
import com.java.boilerplate.dto.auth.DTOAuth;
import com.java.boilerplate.dto.auth.DTOLoginResponse;
import com.java.boilerplate.dto.auth.DTOResetPasswordRequest;
import com.java.boilerplate.dto.users.DTOUser;
import com.java.boilerplate.dto.users.UserViews;
import com.java.boilerplate.model.Users;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthHandler authHandler;

    public AuthController(AuthHandler authHandler) {
        this.authHandler = authHandler;
    }

    @PostMapping("/login")
    public ResponseEntity<DTOLoginResponse> login(
            @RequestBody @Valid DTOAuth data
    ) { return authHandler.login(data); }

    @PostMapping("/register")
    public ResponseEntity<DTOLoginResponse> register(
            @RequestBody @Valid DTOUser data
    ) { return authHandler.register(data); }

    @PostMapping("/forgot")
    public ResponseEntity<Void> generatePasswordResetOtp(
            @RequestParam String email
    ) { return authHandler.generatePasswordResetOtp(email); }

    @PostMapping("/reset")
    public ResponseEntity<DTOLoginResponse> resetPasswordWithOtp(
            @RequestBody DTOResetPasswordRequest request
    ) { return authHandler.resetPasswordWithOtp(request); }

    @GetMapping("/me")
    @JsonView(UserViews.Internal.class)
    public ResponseEntity<DTOUser> getMe(
            @AuthenticationPrincipal Users user
    ) { return ResponseEntity.ok(DTOUser.fromEntity(user)); }
}
