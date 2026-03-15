package com.java.boilerplate.dto.googleOAuth;

import com.java.boilerplate.enums.GenderUser;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DTOGoogleComplete(
        @NotBlank String tempToken,
        @NotBlank String username,
        @NotBlank String password,
        @NotBlank String phone,
        @NotNull GenderUser gender,
        @NotBlank String name,
        String pictureUrl
) {}
