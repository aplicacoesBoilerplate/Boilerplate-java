package com.java.boilerplate.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.java.boilerplate.config.RTokensProperties;
import com.java.boilerplate.config.security.CTokenService;
import com.java.boilerplate.dto.auth.RLoginGoogle;
import com.java.boilerplate.dto.auth.RRespostaLogin;
import com.java.boilerplate.exception.CExceptionsSystem;
import com.java.boilerplate.model.CUsuario;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.UUID;

@Service
public class CGoogleOAuthService {
    private final RTokensProperties tokensProperties;
    private final CUsuarioService usuarioService;
    private final CTokenService tokenService;

    public CGoogleOAuthService(RTokensProperties pTokensProperties, CUsuarioService pUsuarioService, CTokenService pTokenService) {
        this.tokensProperties = pTokensProperties;
        this.usuarioService = pUsuarioService;
        this.tokenService = pTokenService;
    }

    @Transactional
    public RRespostaLogin login(RLoginGoogle pLoginGoogle) {
        GoogleIdToken.Payload payload = validarCredencial(pLoginGoogle.credential());
        String email = payload.getEmail();
        String nome = String.valueOf(payload.get("name"));

        CUsuario usuario;
        try {
            usuario = usuarioService.buscarEntidadePorEmail(email);
        } catch (CExceptionsSystem pException) {
            usuario = usuarioService.criarUsuarioSistema(nome, email, UUID.randomUUID().toString(), "USER", true);
        }

        return new RRespostaLogin(tokenService.gerarToken(usuario));
    }

    private GoogleIdToken.Payload validarCredencial(String pCredential) {
        if (tokensProperties.googleClientId() == null || tokensProperties.googleClientId().isBlank()) {
            throw new CExceptionsSystem("Login com Google não configurado no backend", HttpStatus.BAD_REQUEST);
        }

        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(tokensProperties.googleClientId()))
                    .build();
            GoogleIdToken idToken = verifier.verify(pCredential);

            if (idToken == null) {
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
