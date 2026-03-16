package com.java.boilerplate.dto.googleOAuth;

public record DTOGoogleAuthResponse(
        Boolean isNewUser,
        String token,
        String refreshToken,
        String name,
        String email,
        String picture
) {}