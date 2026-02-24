package com.java.boilerplate.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record DTOResetPasswordRequest(
        @NotBlank(message = "The email field is required")
        String email,

        @NotBlank(message = "The otp code is required")
        String otp,

        @NotBlank(message = "The new password field is required")
        String newPassword
) {}