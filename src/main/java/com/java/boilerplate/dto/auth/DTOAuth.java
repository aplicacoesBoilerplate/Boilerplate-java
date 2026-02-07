package com.java.boilerplate.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DTOAuth(
        @NotBlank(message = "The email or username field is required")
        String usernameOrEmail,

        @NotBlank(message = "The password field is required")
        @Size(min = 8, max = 20, message = "The password field must be between 8 and 20 characters long")
        String password
) {}
