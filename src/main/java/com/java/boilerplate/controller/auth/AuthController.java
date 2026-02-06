package com.java.boilerplate.controller.auth;

import com.fasterxml.jackson.annotation.JsonView;
import com.java.boilerplate.dto.auth.DTOAuth;
import com.java.boilerplate.dto.auth.DTOLoginResponse;
import com.java.boilerplate.dto.users.DTOUser;
import com.java.boilerplate.dto.users.UserViews;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthHandler authHandler;

    public AuthController(AuthHandler authHandler) {
        this.authHandler = authHandler;
    }

    @PostMapping("/login")
    @JsonView(UserViews.Internal.class)
    public ResponseEntity<DTOLoginResponse> login(
            @RequestBody DTOAuth data
    ) { return authHandler.login(data); }

    @PostMapping("/register")
    @JsonView(UserViews.Internal.class)
    public ResponseEntity<DTOLoginResponse> register(
            @RequestBody @JsonView(UserViews.Registration.class) DTOUser data
    ) { return authHandler.register(data); }

    @GetMapping("/me")
    @JsonView(UserViews.Internal.class)
    public ResponseEntity<DTOUser> getMe(
            @RequestHeader("Authorization") String token
    ) { return authHandler.getMe(token); }
}
