package com.java.boilerplate.controller.auth;

import com.fasterxml.jackson.annotation.JsonView;
import com.java.boilerplate.dto.auth.DTOAuth;
import com.java.boilerplate.dto.auth.DTOLoginResponse;
import com.java.boilerplate.dto.auth.DTOOtp;
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
    @JsonView(UserViews.Internal.class)
    public ResponseEntity<DTOUser> register(
            @RequestBody @Valid DTOUser request
    ) { return authHandler.register(request); }

        @PutMapping("/verify")
    public ResponseEntity<DTOLoginResponse> verifyAccount(
            @RequestBody @Valid DTOOtp request
    ) { return authHandler.verifyAccount(request); }

    @PostMapping("/code")
    public ResponseEntity<Void> generateOtpCode(
            @RequestParam String email
    ) { return authHandler.generateOtpCode(email); }

    @PostMapping("/reset")
    public ResponseEntity<DTOLoginResponse> resetPasswordWithOtp(
            @RequestBody DTOOtp request
    ) { return authHandler.resetPasswordWithOtp(request); }

    @GetMapping("/me")
    @JsonView(UserViews.Internal.class)
    public ResponseEntity<DTOUser> getMe(
            @AuthenticationPrincipal Users user
    ) { return ResponseEntity.ok(DTOUser.fromEntity(user)); }
}
