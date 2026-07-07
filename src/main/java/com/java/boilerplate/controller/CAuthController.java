package com.java.boilerplate.controller;

import com.java.boilerplate.dto.auth.RAlteracaoSenha;
import com.java.boilerplate.dto.auth.RConfirmacaoSenha;
import com.java.boilerplate.dto.auth.RLogin;
import com.java.boilerplate.dto.auth.RLoginGoogle;
import com.java.boilerplate.dto.auth.RRedefinicaoSenhaRecuperacao;
import com.java.boilerplate.dto.auth.RRespostaLogin;
import com.java.boilerplate.dto.auth.RRespostaUsuarioAutenticado;
import com.java.boilerplate.dto.auth.RSolicitacaoAcesso;
import com.java.boilerplate.dto.auth.RSolicitacaoRecuperacaoSenha;
import com.java.boilerplate.dto.auth.RVerificacaoCodigoRecuperacaoSenha;
import com.java.boilerplate.service.CAuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class CAuthController {
    private final CAuthService authService;

    public CAuthController(CAuthService pAuthService) {
        this.authService = pAuthService;
    }

    @PostMapping("/login")
    public ResponseEntity<RRespostaLogin> login(@RequestBody @Valid RLogin pLogin) {
        return ResponseEntity.ok(authService.login(pLogin));
    }

    @PostMapping("/login/google")
    public ResponseEntity<RRespostaLogin> loginGoogle(@RequestBody @Valid RLoginGoogle pLoginGoogle) {
        return ResponseEntity.ok(authService.loginGoogle(pLoginGoogle));
    }

    @GetMapping("/me")
    public ResponseEntity<RRespostaUsuarioAutenticado> buscarUsuarioAutenticado() {
        return ResponseEntity.ok(authService.buscarUsuarioAutenticado());
    }

    @PutMapping("/senha")
    public ResponseEntity<Boolean> alterarSenha(@RequestBody @Valid RAlteracaoSenha pAlteracao) {
        return ResponseEntity.ok(authService.alterarSenha(pAlteracao));
    }

    @PostMapping("/senha/confirmar")
    public ResponseEntity<Boolean> confirmarSenha(@RequestBody @Valid RConfirmacaoSenha pConfirmacao) {
        return ResponseEntity.ok(authService.confirmarSenha(pConfirmacao));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/solicitacoes-acesso")
    public ResponseEntity<Boolean> solicitarAcesso(@RequestBody @Valid RSolicitacaoAcesso pSolicitacao) {
        return ResponseEntity.ok(authService.solicitarAcesso(pSolicitacao));
    }

    @PostMapping("/recuperacao-senha/solicitar")
    public ResponseEntity<Boolean> solicitarRecuperacaoSenha(@RequestBody @Valid RSolicitacaoRecuperacaoSenha pSolicitacao) {
        return ResponseEntity.ok(authService.solicitarRecuperacaoSenha(pSolicitacao));
    }

    @PostMapping("/recuperacao-senha/verificar")
    public ResponseEntity<Boolean> verificarCodigoRecuperacaoSenha(@RequestBody @Valid RVerificacaoCodigoRecuperacaoSenha pVerificacao) {
        return ResponseEntity.ok(authService.verificarCodigoRecuperacaoSenha(pVerificacao));
    }

    @PostMapping("/recuperacao-senha/redefinir")
    public ResponseEntity<Boolean> redefinirSenhaRecuperacao(@RequestBody @Valid RRedefinicaoSenhaRecuperacao pRedefinicao) {
        return ResponseEntity.ok(authService.redefinirSenhaRecuperacao(pRedefinicao));
    }
}
