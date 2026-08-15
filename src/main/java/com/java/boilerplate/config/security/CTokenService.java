package com.java.boilerplate.config.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.java.boilerplate.config.RTokensProperties;
import com.java.boilerplate.model.CRefreshToken;
import com.java.boilerplate.model.CUsuario;
import com.java.boilerplate.repository.IRefreshTokenRepository;
import com.java.boilerplate.service.helpers.CHashUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class CTokenService {
    private static final ZoneOffset ZONE_OFFSET_BRASIL = ZoneOffset.of("-03:00");

    private final RTokensProperties properties;
    private final IRefreshTokenRepository refreshTokenRepository;

    public CTokenService(RTokensProperties pProperties, IRefreshTokenRepository pRefreshTokenRepository) {
        this.properties = pProperties;
        this.refreshTokenRepository = pRefreshTokenRepository;
    }

    @Transactional
    public String gerarToken(CUsuario pUsuario) {
        try {
            Algorithm algoritmo = Algorithm.HMAC256(properties.secret());
            Instant expiracao = gerarDataExpiracao();
            String token = JWT.create()
                    .withIssuer(properties.issuer())
                    .withSubject(pUsuario.getEmail())
                    .withJWTId(UUID.randomUUID().toString())
                    .withClaim("id", pUsuario.getIdUsuario())
                    .withClaim("papel", pUsuario.getPapel())
                    .withExpiresAt(expiracao)
                    .sign(algoritmo);
            registrarSessao(pUsuario, token, expiracao);
            return token;
        } catch (JWTCreationException pException) {
            throw new IllegalStateException("Erro ao gerar token JWT", pException);
        }
    }

    @Transactional(readOnly = true)
    public CUsuario validarToken(String pToken) {
        try {
            String token = limparBearer(pToken);
            String subject = JWT.require(Algorithm.HMAC256(properties.secret()))
                    .withIssuer(properties.issuer())
                    .build()
                    .verify(token)
                    .getSubject();
            return refreshTokenRepository.findActiveByHash(CHashUtil.gerarSha256(token), LocalDateTime.now())
                    .map(CRefreshToken::getUsuario)
                    .filter(pUsuario -> subject.equalsIgnoreCase(pUsuario.getEmail()))
                    .filter(CUsuario::isEnabled)
                    .orElse(null);
        } catch (JWTVerificationException pException) {
            return null;
        }
    }

    private Instant gerarDataExpiracao() {
        return LocalDateTime.now()
                .plusMinutes(properties.accessTokenMinutes())
                .toInstant(ZONE_OFFSET_BRASIL);
    }

    private String limparBearer(String pToken) {
        if (pToken == null) {
            return "";
        }
        return pToken.startsWith("Bearer ") ? pToken.substring(7).trim() : pToken.trim();
    }

    @Transactional
    public void revogarToken(String pToken) {
        String token = limparBearer(pToken);
        if (!token.isBlank()) {
            refreshTokenRepository.deleteByTokenHash(CHashUtil.gerarSha256(token));
        }
    }

    @Transactional
    public void revogarSessoesUsuario(Long pIdUsuario) {
        if (pIdUsuario != null && refreshTokenRepository.existsById(pIdUsuario)) {
            refreshTokenRepository.deleteById(pIdUsuario);
        }
    }

    private void registrarSessao(CUsuario pUsuario, String pToken, Instant pExpiracao) {
        CRefreshToken sessao = refreshTokenRepository.findById(pUsuario.getIdUsuario())
                .orElse(new CRefreshToken());
        sessao.setUsuario(pUsuario);
        sessao.setTokenHash(CHashUtil.gerarSha256(pToken));
        sessao.setExpiraEm(LocalDateTime.ofInstant(pExpiracao, ZONE_OFFSET_BRASIL));
        refreshTokenRepository.save(sessao);
    }
}
