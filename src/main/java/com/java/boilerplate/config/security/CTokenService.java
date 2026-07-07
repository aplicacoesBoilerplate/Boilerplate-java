package com.java.boilerplate.config.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.java.boilerplate.config.RTokensProperties;
import com.java.boilerplate.model.CUsuario;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class CTokenService {
    private static final ZoneOffset ZONE_OFFSET_BRASIL = ZoneOffset.of("-03:00");

    private final RTokensProperties properties;

    public CTokenService(RTokensProperties pProperties) {
        this.properties = pProperties;
    }

    public String gerarToken(CUsuario pUsuario) {
        try {
            Algorithm algoritmo = Algorithm.HMAC256(properties.secret());
            return JWT.create()
                    .withIssuer(properties.issuer())
                    .withSubject(pUsuario.getEmail())
                    .withClaim("id", pUsuario.getIdUsuario())
                    .withClaim("papel", pUsuario.getPapel())
                    .withExpiresAt(gerarDataExpiracao())
                    .sign(algoritmo);
        } catch (JWTCreationException pException) {
            throw new IllegalStateException("Erro ao gerar token JWT", pException);
        }
    }

    public String validarToken(String pToken) {
        try {
            return JWT.require(Algorithm.HMAC256(properties.secret()))
                    .withIssuer(properties.issuer())
                    .build()
                    .verify(limparBearer(pToken))
                    .getSubject();
        } catch (JWTVerificationException pException) {
            return "";
        }
    }

    private Instant gerarDataExpiracao() {
        return LocalDateTime.now()
                .plusMinutes(properties.accessTokenMinutes())
                .toInstant(ZONE_OFFSET_BRASIL);
    }

    private String limparBearer(String pToken) {
        return pToken == null ? "" : pToken.replace("Bearer ", "").trim();
    }
}
