package com.java.boilerplate.service;

import com.java.boilerplate.config.security.CTokenService;
import com.java.boilerplate.dto.auth.RAlteracaoSenha;
import com.java.boilerplate.dto.auth.RAtualizacaoPerfilUsuario;
import com.java.boilerplate.dto.auth.RConfirmacaoSenha;
import com.java.boilerplate.dto.auth.RLogin;
import com.java.boilerplate.dto.auth.RLoginGoogle;
import com.java.boilerplate.dto.auth.RRedefinicaoSenhaRecuperacao;
import com.java.boilerplate.dto.auth.RRespostaAtualizacaoPerfilUsuario;
import com.java.boilerplate.dto.auth.RRespostaLogin;
import com.java.boilerplate.dto.auth.RSolicitacaoAcesso;
import com.java.boilerplate.dto.auth.RSolicitacaoRecuperacaoSenha;
import com.java.boilerplate.dto.auth.RVerificacaoCodigoRecuperacaoSenha;
import com.java.boilerplate.dto.rbac.RCargoRbac;
import com.java.boilerplate.dto.usuarios.RUsuario;
import com.java.boilerplate.enums.EStatusSolicitacaoAcesso;
import com.java.boilerplate.exception.CExceptionsSystem;
import com.java.boilerplate.model.CSolicitacaoAcesso;
import com.java.boilerplate.model.CUsuario;
import com.java.boilerplate.repository.ISolicitacaoAcessoRepository;
import com.java.boilerplate.service.helpers.COtpService;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CAuthService implements UserDetailsService {
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final CTokenService tokenService;
    private final CUsuarioService usuarioService;
    private final CRbacService rbacService;
    private final CGoogleOAuthService googleOAuthService;
    private final COtpService otpService;
    private final ISolicitacaoAcessoRepository solicitacaoAcessoRepository;

    public CAuthService(
            @Lazy AuthenticationManager pAuthenticationManager,
            PasswordEncoder pPasswordEncoder,
            CTokenService pTokenService,
            CUsuarioService pUsuarioService,
            CRbacService pRbacService,
            @Lazy CGoogleOAuthService pGoogleOAuthService,
            COtpService pOtpService,
            ISolicitacaoAcessoRepository pSolicitacaoAcessoRepository) {
        this.authenticationManager = pAuthenticationManager;
        this.passwordEncoder = pPasswordEncoder;
        this.tokenService = pTokenService;
        this.usuarioService = pUsuarioService;
        this.rbacService = pRbacService;
        this.googleOAuthService = pGoogleOAuthService;
        this.otpService = pOtpService;
        this.solicitacaoAcessoRepository = pSolicitacaoAcessoRepository;
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
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(pLogin.email(), pLogin.password());
        CUsuario usuario = (CUsuario)
                authenticationManager.authenticate(authenticationToken).getPrincipal();

        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new DisabledException("Usuário inativo");
        }

        return new RRespostaLogin(tokenService.gerarToken(usuario));
    }

    @Transactional
    public RRespostaLogin loginGoogle(RLoginGoogle pLoginGoogle) {
        return googleOAuthService.login(pLoginGoogle);
    }

    @Transactional(readOnly = true)
    public RUsuario buscarUsuarioAutenticado() {
        return usuarioService.buscarPorId(buscarUsuarioLogado().getIdUsuario());
    }

    /**
     * @description Atualiza os dados permitidos para o perfil do usuário autenticado.
     * @param pPerfil Dados do perfil enviados pelo usuário autenticado.
     * @returns Usuário atualizado e token renovado para a sessão.
     */
    @Transactional
    public RRespostaAtualizacaoPerfilUsuario atualizarPerfilUsuarioAutenticado(RAtualizacaoPerfilUsuario pPerfil) {
        CUsuario usuario = buscarUsuarioLogado();
        RUsuario usuarioAtualizado = usuarioService.atualizarPerfil(usuario.getIdUsuario(), pPerfil);

        return new RRespostaAtualizacaoPerfilUsuario(usuarioAtualizado, tokenService.gerarToken(usuario));
    }

    @Transactional(readOnly = true)
    public RCargoRbac buscarCargoUsuarioAutenticado() {
        CUsuario usuario = buscarUsuarioLogado();
        if (usuario.getCargo() == null) {
            throw new CExceptionsSystem("Usuário autenticado não possui cargo vinculado", HttpStatus.FORBIDDEN);
        }

        return rbacService.toDTO(
                rbacService.buscarEntidadePorId(usuario.getCargo().getIdCargo()));
    }

    @Transactional
    public boolean confirmarSenha(RConfirmacaoSenha pConfirmacao) {
        CUsuario usuario = usuarioService.buscarEntidadePorEmail(pConfirmacao.email());
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

        CUsuario usuario = usuarioService.buscarEntidadePorEmail(pAlteracao.emailUser());
        if (!passwordEncoder.matches(pAlteracao.passwordUser(), usuario.getSenha())) {
            throw new CExceptionsSystem("Senha atual inválida", HttpStatus.UNAUTHORIZED);
        }

        validarSenha(pAlteracao.newPassword());
        usuario.setSenha(passwordEncoder.encode(pAlteracao.newPassword()));
        return true;
    }

    @Transactional
    public boolean solicitarAcesso(RSolicitacaoAcesso pSolicitacao) {
        if (!pSolicitacao.senha().equals(pSolicitacao.confirmarSenha())) {
            throw new CExceptionsSystem("A confirmação da senha não confere", HttpStatus.BAD_REQUEST);
        }

        validarSenha(pSolicitacao.senha());
        validarSolicitacaoAcessoDisponivel(pSolicitacao.email());

        CUsuario usuario = usuarioService.criarUsuarioSolicitacaoAcesso(
                pSolicitacao.nome(), pSolicitacao.email(), pSolicitacao.senha());

        CSolicitacaoAcesso solicitacao = new CSolicitacaoAcesso();
        solicitacao.setUsuario(usuario);
        solicitacao.setLiberado(false);
        solicitacao.setStatus(EStatusSolicitacaoAcesso.PENDENTE);
        solicitacaoAcessoRepository.save(solicitacao);
        return true;
    }

    @Transactional
    public boolean solicitarRecuperacaoSenha(RSolicitacaoRecuperacaoSenha pSolicitacao) {
        CUsuario usuario = usuarioService.buscarEntidadePorEmail(pSolicitacao.email());
        otpService.gerarCodigo(usuario);
        return true;
    }

    @Transactional(readOnly = true)
    public boolean verificarCodigoRecuperacaoSenha(RVerificacaoCodigoRecuperacaoSenha pVerificacao) {
        CUsuario usuario = usuarioService.buscarEntidadePorEmail(pVerificacao.email());
        otpService.validarCodigoSemConsumir(usuario, pVerificacao.codigo());
        return true;
    }

    @Transactional
    public boolean redefinirSenhaRecuperacao(RRedefinicaoSenhaRecuperacao pRedefinicao) {
        if (!pRedefinicao.senha().equals(pRedefinicao.confirmarSenha())) {
            throw new CExceptionsSystem("A confirmação da senha não confere", HttpStatus.BAD_REQUEST);
        }

        validarSenha(pRedefinicao.senha());
        CUsuario usuario = usuarioService.buscarEntidadePorEmail(pRedefinicao.email());
        otpService.validarCodigoEConsumir(usuario, pRedefinicao.codigo());
        usuario.setSenha(passwordEncoder.encode(pRedefinicao.senha()));
        usuario.setAtivo(true);
        return true;
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

    private void validarSolicitacaoAcessoDisponivel(String pEmail) {
        usuarioService.buscarEntidadeOpcionalPorEmail(pEmail).ifPresent(pUsuario -> {
            solicitacaoAcessoRepository
                    .findByUsuario_IdUsuario(pUsuario.getIdUsuario())
                    .ifPresent(pSolicitacao -> {
                        if (Boolean.TRUE.equals(pSolicitacao.getLiberado())) {
                            throw new CExceptionsSystem(
                                    "Este usuário já teve o acesso liberado anteriormente", HttpStatus.CONFLICT);
                        }

                        throw new CExceptionsSystem(
                                "Já existe uma solicitação de acesso pendente para esse e-mail", HttpStatus.CONFLICT);
                    });

            throw new CExceptionsSystem("Já existe um usuário cadastrado com esse e-mail", HttpStatus.CONFLICT);
        });
    }

    private void validarSenha(String pSenha) {
        if (pSenha == null || pSenha.length() < 8 || pSenha.length() > 72) {
            throw new CExceptionsSystem("A senha deve ter entre 8 e 72 caracteres", HttpStatus.BAD_REQUEST);
        }
    }
}
