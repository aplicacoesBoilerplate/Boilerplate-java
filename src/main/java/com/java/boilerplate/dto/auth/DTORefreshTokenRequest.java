package com.java.boilerplate.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record DTORefreshTokenRequest(
        @NotBlank(message = "O Refresh Token é obrigatório")
        String refreshToken
) {}
