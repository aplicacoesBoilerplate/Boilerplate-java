package com.java.boilerplate.config.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.java.boilerplate.config.TokensProperties;
import com.java.boilerplate.model.Users;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class TokenService {
    private final TokensProperties tokens;

    public TokenService(TokensProperties tokens) {
        this.tokens = tokens;
    }

    public String generateToken(Users user) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(tokens.getSecret());
            return JWT.create()
                    .withIssuer("boilerplate-api")
                    .withSubject(user.getEmail())
                    .withClaim("id", user.getIdUser())
                    .withClaim("role", user.getRole().name())
                    .withClaim("username", user.getUserUsername())
                    .withExpiresAt(genExpirationDate())
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Error while generating token", exception);
        }
    }

    public String validateToken(String token) {
        try {
            String cleanToken = token.replace("Bearer ", "");
            Algorithm algorithm = Algorithm.HMAC256(tokens.getSecret());
            return JWT.require(algorithm)
                    .withIssuer("boilerplate-api")
                    .build()
                    .verify(cleanToken)
                    .getSubject();
        } catch (JWTVerificationException exception) {
            return "";
        }
    }

    private Instant genExpirationDate() {
        return LocalDateTime.now().plusHours(12).toInstant(ZoneOffset.of("-03:00"));
    }
}
