package com.java.boilerplate.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.java.boilerplate.config.RTokensProperties;
import com.java.boilerplate.config.security.CTokenService;
import com.java.boilerplate.dto.auth.RLoginGoogle;
import com.java.boilerplate.dto.auth.RRespostaLogin;
import com.java.boilerplate.exception.CExceptionsSystem;
import com.java.boilerplate.model.CUsuario;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CGoogleOAuthService {
    private final RTokensProperties tokensProperties;
    private final CUsuarioService usuarioService;
    private final CTokenService tokenService;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;

    public CGoogleOAuthService(
            RTokensProperties pTokensProperties,
            CUsuarioService pUsuarioService,
            CTokenService pTokenService,
            GoogleIdTokenVerifier pGoogleIdTokenVerifier
    ) {
        this.tokensProperties = pTokensProperties;
        this.usuarioService = pUsuarioService;
        this.tokenService = pTokenService;
        this.googleIdTokenVerifier = pGoogleIdTokenVerifier;
    }

    @Transactional
    public RRespostaLogin login(RLoginGoogle pLoginGoogle) {
        GoogleIdToken.Payload payload = validarCredencial(pLoginGoogle.credential());
        String email = payload.getEmail();
        String subject = payload.getSubject();
        CUsuario usuario;
        try {
            usuario = usuarioService.buscarEntidadePorEmail(email);
        } catch (CExceptionsSystem pException) {
            if (pException.getStatus() == HttpStatus.NOT_FOUND) {
                throw new CExceptionsSystem("Identidade do Google não autorizada", HttpStatus.UNAUTHORIZED);
            }
            throw pException;
        }

        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new CExceptionsSystem("Identidade do Google não autorizada", HttpStatus.UNAUTHORIZED);
        }
        usuario = usuarioService.vincularIdentidadeGoogle(usuario, subject);

        return new RRespostaLogin(tokenService.gerarToken(usuario));
    }

    private GoogleIdToken.Payload validarCredencial(String pCredential) {
        if (tokensProperties.googleClientId() == null || tokensProperties.googleClientId().isBlank()) {
            throw new CExceptionsSystem("Login com Google não configurado no backend", HttpStatus.BAD_REQUEST);
        }

        try {
            GoogleIdToken idToken = googleIdTokenVerifier.verify(pCredential);

            if (idToken == null
                    || idToken.getPayload() == null
                    || !Boolean.TRUE.equals(idToken.getPayload().getEmailVerified())
                    || idToken.getPayload().getEmail() == null
                    || idToken.getPayload().getEmail().isBlank()
                    || idToken.getPayload().getSubject() == null
                    || idToken.getPayload().getSubject().isBlank()) {
                throw new CExceptionsSystem("Token do Google inválido", HttpStatus.UNAUTHORIZED);
            }

            return idToken.getPayload();
        } catch (CExceptionsSystem pException) {
            throw pException;
        } catch (Exception pException) {
            throw new CExceptionsSystem("Erro ao autenticar com o Google", HttpStatus.UNAUTHORIZED);
        }
    }
}
