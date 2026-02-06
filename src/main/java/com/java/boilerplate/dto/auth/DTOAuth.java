package com.java.boilerplate.dto.auth;

import jakarta.validation.constraints.Email;

public record DTOAuth(
        @Email
        String usernameOrEmail,
        String password
) {}
