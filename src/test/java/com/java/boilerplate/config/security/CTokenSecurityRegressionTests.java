package com.java.boilerplate.config.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.java.boilerplate.config.RTokensProperties;
import com.java.boilerplate.model.CRefreshToken;
import com.java.boilerplate.model.CCargoRbac;
import com.java.boilerplate.model.CUsuario;
import com.java.boilerplate.repository.IRefreshTokenRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CTokenSecurityRegressionTests {
    private static final String SECRET = "test-secret-with-enough-size-for-hmac";

    @Test
    void jwtAssinadoSemSessaoAtivaDeveSerRejeitado() {
        RTokensProperties properties = new RTokensProperties(SECRET, "issuer-test", 30L, "");
        CTokenService tokenService = new CTokenService(properties, mock(IRefreshTokenRepository.class));
        String tokenForjadoSemSessao = JWT.create()
                .withIssuer("issuer-test")
                .withSubject("admin@example.com")
                .withExpiresAt(Instant.now().plusSeconds(300))
                .sign(Algorithm.HMAC256(SECRET));

        assertThat(tokenService.validarToken(tokenForjadoSemSessao)).isNull();
    }

    @Test
    void logoutDeveRevogarImediatamenteOTokenPersistido() {
        RTokensProperties properties = new RTokensProperties(SECRET, "issuer-test", 30L, "");
        IRefreshTokenRepository repository = mock(IRefreshTokenRepository.class);
        Map<String, CRefreshToken> sessoes = new HashMap<>();
        when(repository.findById(10L)).thenReturn(Optional.empty());
        when(repository.save(any(CRefreshToken.class))).thenAnswer(pInvocation -> {
            CRefreshToken sessao = pInvocation.getArgument(0);
            sessoes.put(sessao.getTokenHash(), sessao);
            return sessao;
        });
        when(repository.findActiveByHash(any(), any())).thenAnswer(pInvocation ->
                Optional.ofNullable(sessoes.get(pInvocation.getArgument(0)))
        );
        org.mockito.Mockito.doAnswer(pInvocation -> {
            sessoes.remove(pInvocation.getArgument(0));
            return null;
        }).when(repository).deleteByTokenHash(any());
        CTokenService tokenService = new CTokenService(properties, repository);
        CUsuario usuario = new CUsuario();
        usuario.setIdUsuario(10L);
        usuario.setEmail("titular@example.com");
        CCargoRbac cargo = new CCargoRbac();
        cargo.setPapel("USER");
        cargo.setAtivo(true);
        usuario.setCargo(cargo);
        usuario.setAtivo(true);
        String token = tokenService.gerarToken(usuario);
        assertThat(tokenService.validarToken(token)).isSameAs(usuario);

        assertThatCode(() -> CTokenService.class
                .getMethod("revogarToken", String.class)
                .invoke(tokenService, token))
                .doesNotThrowAnyException();

        assertThat(tokenService.validarToken(token)).isNull();
    }
}
