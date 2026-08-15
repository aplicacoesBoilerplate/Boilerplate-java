package com.java.boilerplate.service;

import com.java.boilerplate.config.security.CTokenService;
import com.java.boilerplate.config.RRateLimitProperties;
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
import com.java.boilerplate.dto.rbac.RCargoRbac;
import com.java.boilerplate.enums.EStatusSolicitacaoAcesso;
import com.java.boilerplate.exception.CExceptionsSystem;
import com.java.boilerplate.model.CSolicitacaoAcesso;
import com.java.boilerplate.model.CUsuario;
import com.java.boilerplate.repository.ISolicitacaoAcessoRepository;
import com.java.boilerplate.service.helpers.COtpService;
import com.java.boilerplate.service.helpers.CRateLimitService;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.task.TaskRejectedException;

import java.time.Duration;

@Service
public class CAuthService implements UserDetailsService {
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final CTokenService tokenService;
    private final CUsuarioService usuarioService;
    private final CRbacService rbacService;
    private final CGoogleOAuthService googleOAuthService;
    private final COtpService otpService;
    private final CRecoveryService recoveryService;
    private final ISolicitacaoAcessoRepository solicitacaoAcessoRepository;
    private final CRateLimitService rateLimitService;
    private final RRateLimitProperties rateLimitProperties;

    public CAuthService(
            @Lazy AuthenticationManager pAuthenticationManager,
            PasswordEncoder pPasswordEncoder,
            CTokenService pTokenService,
            CUsuarioService pUsuarioService,
            CRbacService pRbacService,
            @Lazy CGoogleOAuthService pGoogleOAuthService,
            COtpService pOtpService,
            CRecoveryService pRecoveryService,
            ISolicitacaoAcessoRepository pSolicitacaoAcessoRepository,
            CRateLimitService pRateLimitService,
            RRateLimitProperties pRateLimitProperties
    ) {
        this.authenticationManager = pAuthenticationManager;
        this.passwordEncoder = pPasswordEncoder;
        this.tokenService = pTokenService;
        this.usuarioService = pUsuarioService;
        this.rbacService = pRbacService;
        this.googleOAuthService = pGoogleOAuthService;
        this.otpService = pOtpService;
        this.recoveryService = pRecoveryService;
        this.solicitacaoAcessoRepository = pSolicitacaoAcessoRepository;
        this.rateLimitService = pRateLimitService;
        this.rateLimitProperties = pRateLimitProperties;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String pUsername) throws UsernameNotFoundException {
        try {
            return usuarioService.buscarEntidadePorEmail(pUsername);
        } catch (CExceptionsSystem pException) {
            throw new UsernameNotFoundException("Usuário não encontrado", pException);
        }
    }

    @Transactional
    public RRespostaLogin login(RLogin pLogin) {
        limitarFluxoPublico("login", pLogin.identificacaoAcesso(), rateLimitProperties.loginAttemptsPerWindow());
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(pLogin.identificacaoAcesso(), pLogin.senha());
        CUsuario usuario = (CUsuario) authenticationManager.authenticate(authenticationToken).getPrincipal();

        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new DisabledException("Usuário inativo");
        }

        rateLimitService.limpar("login:identidade", pLogin.identificacaoAcesso());
        return new RRespostaLogin(tokenService.gerarToken(usuario));
    }

    @Transactional
    public RRespostaLogin loginGoogle(RLoginGoogle pLoginGoogle) {
        limitarFluxoPublico("login-google", pLoginGoogle.credential(), rateLimitProperties.loginAttemptsPerWindow());
        return googleOAuthService.login(pLoginGoogle);
    }

    @Transactional(readOnly = true)
    public RRespostaUsuarioAutenticado buscarUsuarioAutenticado() {
        return new RRespostaUsuarioAutenticado(buscarUsuarioLogado().getIdUsuario());
    }

    @Transactional(readOnly = true)
    public RCargoRbac buscarCargoUsuarioAutenticado() {
        CUsuario usuario = buscarUsuarioLogado();
        if (usuario.getCargo() == null) {
            throw new CExceptionsSystem("Usuário autenticado não possui cargo vinculado", HttpStatus.FORBIDDEN);
        }

        return rbacService.buscarPorId(usuario.getCargo().getIdCargo());
    }

    @Transactional
    public boolean confirmarSenha(RConfirmacaoSenha pConfirmacao) {
        CUsuario usuario = buscarUsuarioLogado();
        if (!pConfirmacao.password().equals(pConfirmacao.confirmPassword())) {
            throw new CExceptionsSystem("A confirmação da senha não confere", HttpStatus.BAD_REQUEST);
        }

        if (!passwordEncoder.matches(pConfirmacao.password(), usuario.getSenha())) {
            throw new CExceptionsSystem("Senha inválida", HttpStatus.UNAUTHORIZED);
        }

        return true;
    }

    @Transactional
    public boolean alterarSenha(RAlteracaoSenha pAlteracao) {
        if (!pAlteracao.newPassword().equals(pAlteracao.confirmNewPassword())) {
            throw new CExceptionsSystem("A confirmação da nova senha não confere", HttpStatus.BAD_REQUEST);
        }

        CUsuario usuario = buscarUsuarioLogado();
        if (!passwordEncoder.matches(pAlteracao.passwordUser(), usuario.getSenha())) {
            throw new CExceptionsSystem("Senha atual inválida", HttpStatus.UNAUTHORIZED);
        }

        validarSenha(pAlteracao.newPassword());
        usuario.setSenha(passwordEncoder.encode(pAlteracao.newPassword()));
        tokenService.revogarSessoesUsuario(usuario.getIdUsuario());
        return true;
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public boolean solicitarAcesso(RSolicitacaoAcesso pSolicitacao) {
        limitarFluxoPublico("solicitacao-acesso", pSolicitacao.email(), rateLimitProperties.publicRequestsPerWindow());
        if (!pSolicitacao.senha().equals(pSolicitacao.confirmarSenha())) {
            throw new CExceptionsSystem("A confirmação da senha não confere", HttpStatus.BAD_REQUEST);
        }

        validarSenha(pSolicitacao.senha());
        if (!solicitacaoAcessoDisponivel(pSolicitacao.email())) {
            return true;
        }

        CUsuario usuario = usuarioService.criarUsuarioSolicitacaoAcesso(
                pSolicitacao.nome(),
                pSolicitacao.email(),
                pSolicitacao.senha()
        );

        CSolicitacaoAcesso solicitacao = new CSolicitacaoAcesso();
        solicitacao.setUsuario(usuario);
        solicitacao.setLiberado(false);
        solicitacao.setStatus(EStatusSolicitacaoAcesso.PENDENTE);
        solicitacaoAcessoRepository.save(solicitacao);
        return true;
    }

    public boolean solicitarRecuperacaoSenha(RSolicitacaoRecuperacaoSenha pSolicitacao) {
        limitarFluxoPublico("recuperacao-solicitar", pSolicitacao.email(), rateLimitProperties.loginAttemptsPerWindow());
        try {
            recoveryService.solicitar(pSolicitacao.email());
        } catch (TaskRejectedException pException) {
            // Mantem a resposta publica uniforme quando a fila limitada esta cheia.
        }
        return true;
    }

    @Transactional(noRollbackFor = CExceptionsSystem.class)
    public boolean verificarCodigoRecuperacaoSenha(RVerificacaoCodigoRecuperacaoSenha pVerificacao) {
        limitarFluxoPublico("recuperacao-verificar", pVerificacao.email(), rateLimitProperties.loginAttemptsPerWindow());
        CUsuario usuario = buscarUsuarioRecuperacao(pVerificacao.email());
        otpService.validarCodigoSemConsumir(usuario, pVerificacao.codigo());
        return true;
    }

    @Transactional(noRollbackFor = CExceptionsSystem.class)
    public boolean redefinirSenhaRecuperacao(RRedefinicaoSenhaRecuperacao pRedefinicao) {
        limitarFluxoPublico("recuperacao-redefinir", pRedefinicao.email(), rateLimitProperties.loginAttemptsPerWindow());
        if (!pRedefinicao.senha().equals(pRedefinicao.confirmarSenha())) {
            throw new CExceptionsSystem("A confirmação da senha não confere", HttpStatus.BAD_REQUEST);
        }

        validarSenha(pRedefinicao.senha());
        CUsuario usuario = buscarUsuarioRecuperacao(pRedefinicao.email());
        otpService.validarCodigoEConsumir(usuario, pRedefinicao.codigo());
        usuario.setSenha(passwordEncoder.encode(pRedefinicao.senha()));
        tokenService.revogarSessoesUsuario(usuario.getIdUsuario());
        return true;
    }

    @Transactional
    public void logout(String pAuthorizationHeader) {
        tokenService.revogarToken(pAuthorizationHeader);
    }

    @Transactional(readOnly = true)
    public CUsuario buscarUsuarioLogado() {
        Object principal = SecurityContextHolder.getContext().getAuthentication() == null
                ? null
                : SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof CUsuario usuario) {
            return usuarioService.buscarEntidadePorId(usuario.getIdUsuario());
        }

        throw new CExceptionsSystem("Usuário não autenticado", HttpStatus.UNAUTHORIZED);
    }

    private boolean solicitacaoAcessoDisponivel(String pEmail) {
        try {
            CUsuario usuario = usuarioService.buscarEntidadePorEmail(pEmail);
            solicitacaoAcessoRepository.findByUsuario_IdUsuario(usuario.getIdUsuario());
            return false;
        } catch (CExceptionsSystem pException) {
            if (pException.getStatus() == HttpStatus.NOT_FOUND) {
                return true;
            }
            throw pException;
        }
    }

    private void validarSenha(String pSenha) {
        if (pSenha == null || pSenha.length() < 8 || pSenha.length() > 72) {
            throw new CExceptionsSystem("A senha deve ter entre 8 e 72 caracteres", HttpStatus.BAD_REQUEST);
        }
    }

    private CUsuario buscarUsuarioRecuperacao(String pEmail) {
        try {
            return usuarioService.buscarEntidadePorEmail(pEmail);
        } catch (CExceptionsSystem pException) {
            if (pException.getStatus() == HttpStatus.NOT_FOUND) {
                throw new CExceptionsSystem("Código de recuperação inválido ou expirado", HttpStatus.UNAUTHORIZED);
            }
            throw pException;
        }
    }

    private void limitarFluxoPublico(String pEscopo, String pIdentidade, int pLimiteIdentidade) {
        Duration janela = Duration.ofSeconds(rateLimitProperties.windowSeconds());
        rateLimitService.consumir(pEscopo + ":identidade", pIdentidade, pLimiteIdentidade, janela);
    }
}
