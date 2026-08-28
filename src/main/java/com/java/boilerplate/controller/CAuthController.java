package com.java.boilerplate.controller;

import com.java.boilerplate.config.security.CSpaCsrfTokenRequestHandler;
import com.java.boilerplate.dto.auth.RContextoSessaoBff;
import com.java.boilerplate.dto.auth.RLogin;
import com.java.boilerplate.dto.auth.RLoginBff;
import com.java.boilerplate.dto.auth.RLoginGoogle;
import com.java.boilerplate.dto.auth.RLoginGoogleBff;
import com.java.boilerplate.dto.auth.RRedefinicaoSenhaRecuperacao;
import com.java.boilerplate.dto.auth.RRespostaCsrfBff;
import com.java.boilerplate.dto.auth.RRespostaLogin;
import com.java.boilerplate.dto.auth.RSolicitacaoAcesso;
import com.java.boilerplate.dto.auth.RSolicitacaoRecuperacaoSenha;
import com.java.boilerplate.dto.auth.RVerificacaoCodigoRecuperacaoSenha;
import com.java.boilerplate.service.CAuthBffService;
import com.java.boilerplate.service.CAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class CAuthController {
    private final CAuthService authService;
    private final CAuthBffService authBffService;

    public CAuthController(CAuthService pAuthService, CAuthBffService pAuthBffService) {
        this.authService = pAuthService;
        this.authBffService = pAuthBffService;
    }

    @PostMapping("/login")
    public ResponseEntity<RContextoSessaoBff> login(@RequestBody @Valid RLoginBff pLogin, HttpServletRequest pRequest) {
        return ResponseEntity.ok(authBffService.login(pLogin, pRequest));
    }

    @PostMapping("/login/google")
    public ResponseEntity<RContextoSessaoBff> loginGoogle(
            @RequestBody @Valid RLoginGoogleBff pLogin, HttpServletRequest pRequest) {
        return ResponseEntity.ok(authBffService.loginGoogle(pLogin, pRequest));
    }

    @GetMapping("/session")
    public ResponseEntity<RContextoSessaoBff> buscarSessao() {
        return ResponseEntity.ok(authBffService.obterContextoAutenticado());
    }

    @GetMapping("/csrf")
    public ResponseEntity<RRespostaCsrfBff> csrf(CsrfToken pCsrfToken, HttpServletRequest pRequest) {
        Object tokenBruto = pRequest.getAttribute(CSpaCsrfTokenRequestHandler.ATRIBUTO_TOKEN_BRUTO);
        CsrfToken csrfToken = tokenBruto instanceof CsrfToken token ? token : pCsrfToken;
        return ResponseEntity.ok(new RRespostaCsrfBff(
                csrfToken.getToken(), csrfToken.getHeaderName(), csrfToken.getParameterName()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest pRequest, HttpServletResponse pResponse) {
        authBffService.logout(pRequest, pResponse);
        return ResponseEntity.noContent().build();
    }

    @PostMapping({"/cadastro", "/solicitacoes-acesso"})
    public ResponseEntity<Void> cadastrar(@RequestBody @Valid RSolicitacaoAcesso pSolicitacao) {
        authBffService.cadastrar(pSolicitacao);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/recuperacao-senha/solicitar")
    public ResponseEntity<Void> solicitarRecuperacao(@RequestBody @Valid RSolicitacaoRecuperacaoSenha pSolicitacao) {
        authBffService.solicitarRecuperacao(pSolicitacao);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/recuperacao-senha/verificar")
    public ResponseEntity<Void> verificarRecuperacao(@RequestBody @Valid RVerificacaoCodigoRecuperacaoSenha pVerificacao) {
        authBffService.verificarRecuperacao(pVerificacao);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/recuperacao-senha/redefinir")
    public ResponseEntity<Void> redefinirSenha(@RequestBody @Valid RRedefinicaoSenhaRecuperacao pRedefinicao) {
        authBffService.redefinirSenha(pRedefinicao);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/token/login")
    public ResponseEntity<RRespostaLogin> loginJwt(@RequestBody @Valid RLogin pLogin) {
        return ResponseEntity.ok(authService.login(pLogin));
    }

    @PostMapping("/token/login/google")
    public ResponseEntity<RRespostaLogin> loginGoogleJwt(@RequestBody @Valid RLoginGoogle pLogin) {
        return ResponseEntity.ok(authService.loginGoogle(pLogin));
    }

    @PostMapping("/token/logout")
    public ResponseEntity<Void> logoutJwt(@RequestHeader(value = "Authorization", required = false) String pAuthorizationHeader) {
        authService.logout(pAuthorizationHeader);
        return ResponseEntity.noContent().build();
    }
}
