package com.java.boilerplate.config.security;

import com.java.boilerplate.config.RTokensProperties;
import com.java.boilerplate.model.CRefreshToken;
import com.java.boilerplate.model.CCargoRbac;
import com.java.boilerplate.model.CUsuario;
import com.java.boilerplate.repository.IRefreshTokenRepository;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEObject;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CTokenSecurityRegressionTests {
    private static final String ENCRYPTION_KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Test
    void jweSemSessaoAtivaDeveSerRejeitado() {
        RTokensProperties properties = new RTokensProperties(ENCRYPTION_KEY, "", "issuer-test", 30L, "");
        CTokenService tokenService = new CTokenService(properties, mock(IRefreshTokenRepository.class));

        assertThat(tokenService.validarToken(tokenService.gerarToken(usuario(10L, "admin@example.com")))).isNull();
    }

    @Test
    void tokenEmitidoDeveSerJweCriptografadoELogoutDeveRevogaLoImediatamente() throws Exception {
        RTokensProperties properties = new RTokensProperties(ENCRYPTION_KEY, "", "issuer-test", 30L, "");
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
        CUsuario usuario = usuario(10L, "titular@example.com");
        String token = tokenService.gerarToken(usuario);

        JWEObject jwe = JWEObject.parse(token);
        assertThat(token.split("\\.")).hasSize(5);
        assertThat(jwe.getHeader().getAlgorithm()).isEqualTo(JWEAlgorithm.DIR);
        assertThat(jwe.getHeader().getEncryptionMethod()).isEqualTo(EncryptionMethod.A256GCM);
        assertThat(token).doesNotContain("titular@example.com");
        assertThat(tokenService.validarToken(token)).isSameAs(usuario);

        assertThatCode(() -> CTokenService.class
                .getMethod("revogarToken", String.class)
                .invoke(tokenService, token))
                .doesNotThrowAnyException();

        assertThat(tokenService.validarToken(token)).isNull();
    }

    @Test
    void tokenJweAdulteradoDeveSerRejeitado() {
        RTokensProperties properties = new RTokensProperties(ENCRYPTION_KEY, "", "issuer-test", 30L, "");
        CTokenService tokenService = new CTokenService(properties, mock(IRefreshTokenRepository.class));
        String token = tokenService.gerarToken(usuario(10L, "titular@example.com"));
        String tokenAdulterado = token.substring(0, token.length() - 1) + (token.endsWith("A") ? "B" : "A");

        assertThat(tokenService.validarToken(tokenAdulterado)).isNull();
    }

    private CUsuario usuario(Long pIdUsuario, String pEmail) {
        CUsuario usuario = new CUsuario();
        usuario.setIdUsuario(pIdUsuario);
        usuario.setEmail(pEmail);
        CCargoRbac cargo = new CCargoRbac();
        cargo.setPapel("USER");
        cargo.setAtivo(true);
        usuario.setCargo(cargo);
        usuario.setAtivo(true);
        return usuario;
    }
}
