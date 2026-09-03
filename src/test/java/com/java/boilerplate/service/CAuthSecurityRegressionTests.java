package com.java.boilerplate.service;

import com.java.boilerplate.config.security.CTokenService;
import com.java.boilerplate.config.RRateLimitProperties;
import com.java.boilerplate.dto.auth.RAlteracaoSenha;
import com.java.boilerplate.dto.auth.RConfirmacaoSenha;
import com.java.boilerplate.dto.auth.RRedefinicaoSenhaRecuperacao;
import com.java.boilerplate.dto.auth.RSolicitacaoRecuperacaoSenha;
import com.java.boilerplate.model.CUsuario;
import com.java.boilerplate.repository.ISolicitacaoAcessoRepository;
import com.java.boilerplate.service.helpers.COtpService;
import com.java.boilerplate.service.helpers.CRateLimitService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CAuthSecurityRegressionTests {
    @Mock
    private org.springframework.security.authentication.AuthenticationManager authenticationManager;
    @Mock
    private CTokenService tokenService;
    @Mock
    private CUsuarioService usuarioService;
    @Mock
    private CRbacService rbacService;
    @Mock
    private CGoogleOAuthService googleOAuthService;
    @Mock
    private COtpService otpService;
    @Mock
    private CRecoveryService recoveryService;
    @Mock
    private ISolicitacaoAcessoRepository solicitacaoAcessoRepository;
    @Mock
    private CRateLimitService rateLimitService;

    private PasswordEncoder passwordEncoder;
    private CAuthService authService;

    @BeforeEach
    void configurar() {
        passwordEncoder = new BCryptPasswordEncoder(4);
        authService = new CAuthService(
                authenticationManager,
                passwordEncoder,
                tokenService,
                usuarioService,
                rbacService,
                googleOAuthService,
                otpService,
                recoveryService,
                solicitacaoAcessoRepository,
                rateLimitService,
                new RRateLimitProperties(60, 30, 5, 10_000),
                ativacaoPrimeiroAcessoService
        );
    }

    @AfterEach
    void limparContextoDeSeguranca() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void alterarSenhaDeveIgnorarEmailDoCorpoEVincularMudancaAoPrincipalAutenticado() {
        CUsuario usuarioAutenticado = usuario(10L, "titular@example.com", "senha-atual");
        CUsuario outraConta = usuario(20L, "alvo@example.com", "senha-alvo");
        autenticar(usuarioAutenticado);
        when(usuarioService.buscarEntidadePorId(10L)).thenReturn(usuarioAutenticado);

        assertThatCode(() -> authService.alterarSenha(new RAlteracaoSenha(
                        "alvo@example.com",
                        "senha-atual",
                        "senha-nova-segura",
                        "senha-nova-segura"
                )))
                .doesNotThrowAnyException();

        assertThat(passwordEncoder.matches("senha-nova-segura", usuarioAutenticado.getSenha())).isTrue();
        assertThat(passwordEncoder.matches("senha-alvo", outraConta.getSenha())).isTrue();
    }

    @Test
    void confirmarSenhaDeveValidarSomenteAPrincipalAutenticado() {
        CUsuario usuarioAutenticado = usuario(10L, "titular@example.com", "senha-atual");
        CUsuario outraConta = usuario(20L, "alvo@example.com", "senha-alvo");
        autenticar(usuarioAutenticado);
        when(usuarioService.buscarEntidadePorId(10L)).thenReturn(usuarioAutenticado);

        assertThatCode(() -> assertThat(authService.confirmarSenha(new RConfirmacaoSenha(
                        "alvo@example.com",
                        "senha-atual",
                        "senha-atual"
                )))
                .isTrue())
                .doesNotThrowAnyException();
    }

    @Test
    void redefinirSenhaNaoDeveReativarContaPendenteOuDesabilitada() {
        CUsuario usuario = usuario(30L, "pendente@example.com", "senha-antiga");
        usuario.setAtivo(false);
        when(usuarioService.buscarEntidadePorEmail("pendente@example.com")).thenReturn(usuario);

        authService.redefinirSenhaRecuperacao(new RRedefinicaoSenhaRecuperacao(
                "pendente@example.com",
                "123456",
                "senha-nova-segura",
                "senha-nova-segura"
        ));

        assertThat(usuario.getAtivo()).isFalse();
        assertThat(passwordEncoder.matches("senha-nova-segura", usuario.getSenha())).isTrue();
    }

    @Test
    void solicitarRecuperacaoDeContaInexistenteDeveManterRespostaUniforme() {
        assertThatCode(() -> assertThat(authService.solicitarRecuperacaoSenha(
                        new RSolicitacaoRecuperacaoSenha("ausente@example.com")
                ))
                .isTrue())
                .doesNotThrowAnyException();
        verify(recoveryService).solicitar("ausente@example.com");
        verify(usuarioService, never()).buscarEntidadePorEmail(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void solicitarAcessoDeEmailJaCadastradoDeveManterRespostaUniforme() {
        CUsuario existente = usuario(40L, "existente@example.com", "senha-atual");
        when(usuarioService.buscarEntidadePorEmail("existente@example.com")).thenReturn(existente);
        when(solicitacaoAcessoRepository.findByUsuario_IdUsuario(40L)).thenReturn(java.util.Optional.empty());

        assertThatCode(() -> assertThat(authService.solicitarAcesso(
                        new com.java.boilerplate.dto.auth.RSolicitacaoAcesso(
                                "Existente",
                                "existente@example.com",
                                "senha-nova-segura",
                                "senha-nova-segura"
                        )
                ))
                .isTrue())
                .doesNotThrowAnyException();
        verify(usuarioService, never()).criarUsuarioSolicitacaoAcesso(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    private CUsuario usuario(Long pId, String pEmail, String pSenha) {
        CUsuario usuario = new CUsuario();
        usuario.setIdUsuario(pId);
        usuario.setEmail(pEmail);
        usuario.setSenha(passwordEncoder.encode(pSenha));
        usuario.setAtivo(true);
        return usuario;
    }

    private void autenticar(CUsuario pUsuario) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(pUsuario, null, pUsuario.getAuthorities())
        );
    }
}
