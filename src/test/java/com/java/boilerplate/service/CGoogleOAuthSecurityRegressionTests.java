package com.java.boilerplate.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.java.boilerplate.config.RTokensProperties;
import com.java.boilerplate.config.security.CTokenService;
import com.java.boilerplate.dto.auth.RLoginGoogle;
import com.java.boilerplate.dto.auth.RRespostaLogin;
import com.java.boilerplate.model.CUsuario;
import com.java.boilerplate.exception.CExceptionsSystem;
import org.springframework.http.HttpStatus;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CGoogleOAuthSecurityRegressionTests {
    @Test
    void loginDeveReutilizarVerifierInjetadoEmVezDeCriarTransportePorRequisicao() throws Exception {
        RTokensProperties properties = new RTokensProperties(
                "test-secret-with-enough-size-for-hmac",
                "issuer-test",
                30L,
                "google-client-id"
        );
        CUsuarioService usuarioService = mock(CUsuarioService.class);
        CTokenService tokenService = mock(CTokenService.class);
        GoogleIdTokenVerifier verifier = mock(GoogleIdTokenVerifier.class);
        GoogleIdToken idToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmail("titular@example.com");
        payload.setEmailVerified(true);
        payload.setSubject("google-subject-123");
        payload.set("name", "Titular");
        CUsuario usuario = new CUsuario();
        usuario.setEmail("titular@example.com");
        when(verifier.verify("credencial-google")).thenReturn(idToken);
        when(idToken.getPayload()).thenReturn(payload);
        when(usuarioService.buscarEntidadePorEmail("titular@example.com")).thenReturn(usuario);
        when(usuarioService.vincularIdentidadeGoogle(usuario, "google-subject-123")).thenReturn(usuario);
        when(tokenService.gerarToken(usuario)).thenReturn("jwt-local");
        AtomicReference<RRespostaLogin> resposta = new AtomicReference<>();

        assertThatCode(() -> {
            var constructor = CGoogleOAuthService.class.getConstructor(
                    RTokensProperties.class,
                    CUsuarioService.class,
                    CTokenService.class,
                    GoogleIdTokenVerifier.class
            );
            CGoogleOAuthService service = constructor.newInstance(properties, usuarioService, tokenService, verifier);
            resposta.set(service.login(new RLoginGoogle("credencial-google")));
        }).doesNotThrowAnyException();

        assertThat(resposta.get().tokenJWT()).isEqualTo("jwt-local");
        verify(verifier).verify("credencial-google");
    }

    @Test
    void loginDeveRejeitarEmailGoogleNaoVerificado() throws Exception {
        RTokensProperties properties = properties();
        CUsuarioService usuarioService = mock(CUsuarioService.class);
        CTokenService tokenService = mock(CTokenService.class);
        GoogleIdTokenVerifier verifier = mock(GoogleIdTokenVerifier.class);
        GoogleIdToken idToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmail("titular@example.com");
        payload.setEmailVerified(false);
        payload.setSubject("google-subject-123");
        when(verifier.verify("credencial-google")).thenReturn(idToken);
        when(idToken.getPayload()).thenReturn(payload);
        CGoogleOAuthService service = new CGoogleOAuthService(properties, usuarioService, tokenService, verifier);

        assertThatThrownBy(() -> service.login(new RLoginGoogle("credencial-google")))
                .isInstanceOf(CExceptionsSystem.class)
                .extracting(pErro -> ((CExceptionsSystem) pErro).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(usuarioService, never()).criarUsuarioSistema(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void loginDeIdentidadeGoogleDesconhecidaNaoDeveCriarUsuarioAtivo() throws Exception {
        RTokensProperties properties = properties();
        CUsuarioService usuarioService = mock(CUsuarioService.class);
        CTokenService tokenService = mock(CTokenService.class);
        GoogleIdTokenVerifier verifier = mock(GoogleIdTokenVerifier.class);
        GoogleIdToken idToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmail("desconhecido@example.com");
        payload.setEmailVerified(true);
        payload.setSubject("google-subject-456");
        when(verifier.verify("credencial-google")).thenReturn(idToken);
        when(idToken.getPayload()).thenReturn(payload);
        when(usuarioService.buscarEntidadePorEmail("desconhecido@example.com"))
                .thenThrow(new CExceptionsSystem("Usuário não encontrado", HttpStatus.NOT_FOUND));
        CGoogleOAuthService service = new CGoogleOAuthService(properties, usuarioService, tokenService, verifier);

        assertThatThrownBy(() -> service.login(new RLoginGoogle("credencial-google")))
                .isInstanceOf(CExceptionsSystem.class)
                .extracting(pErro -> ((CExceptionsSystem) pErro).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(usuarioService, never()).criarUsuarioSistema(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    private RTokensProperties properties() {
        return new RTokensProperties(
                "test-secret-with-enough-size-for-hmac",
                "issuer-test",
                30L,
                "google-client-id"
        );
    }
}
