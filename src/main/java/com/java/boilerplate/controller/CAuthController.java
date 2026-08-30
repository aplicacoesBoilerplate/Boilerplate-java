package com.java.boilerplate.controller;

import com.java.boilerplate.dto.auth.RLogin;
import com.java.boilerplate.dto.auth.RLoginGoogle;
import com.java.boilerplate.dto.auth.RAlteracaoSenha;
import com.java.boilerplate.dto.auth.RConfirmacaoSenha;
import com.java.boilerplate.dto.auth.RRedefinicaoSenhaRecuperacao;
import com.java.boilerplate.dto.auth.RRespostaLogin;
import com.java.boilerplate.dto.auth.RSolicitacaoAcesso;
import com.java.boilerplate.dto.auth.RSolicitacaoRecuperacaoSenha;
import com.java.boilerplate.dto.auth.RVerificacaoCodigoRecuperacaoSenha;
import com.java.boilerplate.service.CAuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/auth")
public class CAuthController {
    private final CAuthService authService;

    public CAuthController(CAuthService pAuthService) {
        this.authService = pAuthService;
    }

    @PostMapping({"/login", "/token/login"})
    public ResponseEntity<RRespostaLogin> login(@RequestBody @Valid RLogin pLogin) {
        return ResponseEntity.ok(authService.login(pLogin));
    }

    @PostMapping({"/login/google", "/token/login/google"})
    public ResponseEntity<RRespostaLogin> loginGoogle(@RequestBody @Valid RLoginGoogle pLogin) {
        return ResponseEntity.ok(authService.loginGoogle(pLogin));
    }

    @GetMapping("/me")
    public ResponseEntity<?> buscarUsuarioAutenticado() {
        return ResponseEntity.ok(authService.buscarUsuarioAutenticado());
    }

    @GetMapping("/me/cargo")
    public ResponseEntity<?> buscarCargoUsuarioAutenticado() {
        return ResponseEntity.ok(authService.buscarCargoUsuarioAutenticado());
    }

    @PostMapping("/senha/confirmar")
    public ResponseEntity<Boolean> confirmarSenha(@RequestBody @Valid RConfirmacaoSenha pConfirmacao) {
        return ResponseEntity.ok(authService.confirmarSenha(pConfirmacao));
    }

    @PutMapping("/senha")
    public ResponseEntity<Boolean> alterarSenha(@RequestBody @Valid RAlteracaoSenha pAlteracao) {
        return ResponseEntity.ok(authService.alterarSenha(pAlteracao));
    }

    @PostMapping({"/logout", "/token/logout"})
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String pAuthorizationHeader) {
        authService.logout(pAuthorizationHeader);
        return ResponseEntity.noContent().build();
    }

    @PostMapping({"/cadastro", "/solicitacoes-acesso"})
    public ResponseEntity<Void> cadastrar(@RequestBody @Valid RSolicitacaoAcesso pSolicitacao) {
        authService.solicitarAcesso(pSolicitacao);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/recuperacao-senha/solicitar")
    public ResponseEntity<Void> solicitarRecuperacao(@RequestBody @Valid RSolicitacaoRecuperacaoSenha pSolicitacao) {
        authService.solicitarRecuperacaoSenha(pSolicitacao);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/recuperacao-senha/verificar")
    public ResponseEntity<Void> verificarRecuperacao(@RequestBody @Valid RVerificacaoCodigoRecuperacaoSenha pVerificacao) {
        authService.verificarCodigoRecuperacaoSenha(pVerificacao);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/recuperacao-senha/redefinir")
    public ResponseEntity<Void> redefinirSenha(@RequestBody @Valid RRedefinicaoSenhaRecuperacao pRedefinicao) {
        authService.redefinirSenhaRecuperacao(pRedefinicao);
        return ResponseEntity.noContent().build();
    }

}
