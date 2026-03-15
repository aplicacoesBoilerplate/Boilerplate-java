package com.java.boilerplate.dto.googleOAuth;

public record DTOGoogleAuthResponse(
        Boolean isNewUser,
        String accessToken,
        String refreshToken,
        String name,
        String email,
        String picture
) {}