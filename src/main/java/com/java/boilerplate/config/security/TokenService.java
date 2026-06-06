package com.java.boilerplate.config.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.java.boilerplate.config.TokensProperties;
import com.java.boilerplate.model.Users;
import com.java.boilerplate.service.context.AppContextService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class TokenService {
    private final TokensProperties tokens;
    private final AppContextService appContextService;

    public TokenService(TokensProperties tokens, AppContextService appContextService) {
        this.tokens = tokens;
        this.appContextService = appContextService;
    }

    public String generateToken(Users user) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(tokens.getSecret());
            return JWT.create()
                    .withIssuer("tzencontros-api")
                    .withSubject(user.getEmail())
                    .withClaim("id", user.getIdUser())
                    .withClaim("role", user.getRole().name())
                    .withClaim("username", user.getUserUsername())
                    .withClaim("contextKey", user.getContextKey())
                    .withExpiresAt(genExpirationDate())
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Error while generating token", exception);
        }
    }

    public String validateToken(String token) {
        try {
            return this.decodeToken(token, "tzencontros-api").getSubject();
        } catch (JWTVerificationException exception) {
            return "";
        }
    }

    public String getContextKey(String token) {
        try {
            return this.decodeToken(token, "tzencontros-api").getClaim("contextKey").asString();
        } catch (JWTVerificationException exception) {
            return "";
        }
    }

    private Instant genExpirationDate() {
        return LocalDateTime.now().plusHours(12).toInstant(ZoneOffset.of("-03:00"));
    }

    public String generateTempRegistrationToken(String email) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(tokens.getSecret());
            return JWT.create()
                    .withIssuer("tzencontros-api-temp")
                    .withSubject(email)
                    .withClaim("contextKey", appContextService.getCurrentKey())
                    .withExpiresAt(LocalDateTime.now().plusMinutes(15).toInstant(ZoneOffset.of("-03:00")))
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Error generating temp token", exception);
        }
    }

    public String validateTempRegistrationToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(tokens.getSecret());
            DecodedJWT jwt = JWT.require(algorithm)
                    .withIssuer("tzencontros-api-temp")
                    .build()
                    .verify(token);

            String tokenContextKey = jwt.getClaim("contextKey").asString();
            if (!appContextService.getCurrentKey().equals(tokenContextKey)) return null;

            return jwt.getSubject();
        } catch (JWTVerificationException exception) {
            return null;
        }
    }

    private DecodedJWT decodeToken(String token, String issuer) {
        String cleanToken = token.replace("Bearer ", "");
        Algorithm algorithm = Algorithm.HMAC256(tokens.getSecret());
        return JWT.require(algorithm)
                .withIssuer(issuer)
                .build()
                .verify(cleanToken);
    }
}
