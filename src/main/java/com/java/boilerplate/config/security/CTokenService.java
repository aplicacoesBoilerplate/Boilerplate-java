package com.java.boilerplate.config.security;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jose.crypto.DirectEncrypter;
import com.nimbusds.jwt.JWTClaimsSet;
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
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.text.ParseException;

@Service
public class CTokenService {
    private static final ZoneOffset ZONE_OFFSET_BRASIL = ZoneOffset.of("-03:00");

    private final RTokensProperties properties;
    private final IRefreshTokenRepository refreshTokenRepository;
    private final SecretKey encryptionKey;

    public CTokenService(RTokensProperties pProperties, IRefreshTokenRepository pRefreshTokenRepository) {
        this.properties = pProperties;
        this.refreshTokenRepository = pRefreshTokenRepository;
        this.encryptionKey = new SecretKeySpec(Base64.getDecoder().decode(pProperties.encryptionKey()), "AES");
    }

    @Transactional
    public String gerarToken(CUsuario pUsuario) {
        try {
            Instant expiracao = gerarDataExpiracao();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(properties.issuer())
                    .subject(pUsuario.getEmail())
                    .jwtID(UUID.randomUUID().toString())
                    .claim("id", pUsuario.getIdUsuario())
                    .claim("papel", pUsuario.getPapel())
                    .issueTime(Date.from(Instant.now()))
                    .expirationTime(Date.from(expiracao))
                    .build();
            JWEObject jwe = new JWEObject(
                    new JWEHeader.Builder(JWEAlgorithm.DIR, EncryptionMethod.A256GCM).contentType("JWT").build(),
                    new Payload(claims.toJSONObject())
            );
            jwe.encrypt(new DirectEncrypter(encryptionKey));
            String token = jwe.serialize();
            registrarSessao(pUsuario, token, expiracao);
            return token;
        } catch (JOSEException pException) {
            throw new IllegalStateException("Erro ao gerar token JWE", pException);
        }
    }

    @Transactional(readOnly = true)
    public CUsuario validarToken(String pToken) {
        try {
            String token = limparBearer(pToken);
            JWEObject jwe = JWEObject.parse(token);
            if (!JWEAlgorithm.DIR.equals(jwe.getHeader().getAlgorithm())
                    || !EncryptionMethod.A256GCM.equals(jwe.getHeader().getEncryptionMethod())) {
                return null;
            }
            jwe.decrypt(new DirectDecrypter(encryptionKey));
            JWTClaimsSet claims = JWTClaimsSet.parse(jwe.getPayload().toJSONObject());
            if (!properties.issuer().equals(claims.getIssuer())
                    || claims.getSubject() == null
                    || claims.getJWTID() == null
                    || claims.getExpirationTime() == null
                    || !claims.getExpirationTime().after(new Date())) {
                return null;
            }
            return refreshTokenRepository.findActiveByHash(CHashUtil.gerarSha256(token), LocalDateTime.now())
                    .map(CRefreshToken::getUsuario)
                    .filter(pUsuario -> claims.getSubject().equalsIgnoreCase(pUsuario.getEmail()))
                    .filter(CUsuario::isEnabled)
                    .orElse(null);
        } catch (ParseException | JOSEException pException) {
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
