package com.java.boilerplate.service;

import com.java.boilerplate.config.RSoftwareCenterProperties;
import com.java.boilerplate.config.RSessaoCookieProperties;
import com.java.boilerplate.config.security.RPrincipalSessaoBff;
import com.java.boilerplate.dto.auth.RContextoSessaoBff;
import com.java.boilerplate.dto.auth.RLoginBff;
import com.java.boilerplate.dto.auth.RLoginGoogleBff;
import com.java.boilerplate.dto.auth.RRedefinicaoSenhaRecuperacao;
import com.java.boilerplate.dto.auth.RSolicitacaoAcesso;
import com.java.boilerplate.dto.auth.RSolicitacaoRecuperacaoSenha;
import com.java.boilerplate.dto.auth.RVerificacaoCodigoRecuperacaoSenha;
import com.java.boilerplate.exception.CExceptionsSystem;
import com.java.boilerplate.integration.softwarecenter.CSoftwareCenterClientException;
import com.java.boilerplate.integration.softwarecenter.ISoftwareCenterClient;
import com.java.boilerplate.integration.softwarecenter.RSoftwareCenterDtos;
import com.java.boilerplate.integration.softwarecenter.RSoftwareCenterDtos.RContextoSessao;
import com.java.boilerplate.integration.softwarecenter.RSoftwareCenterDtos.RSessaoCriada;
import com.java.boilerplate.integration.softwarecenter.RSoftwareCenterDtos.RSessaoValidada;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * @description Orquestra a sessão local do BFF sem expor a sessão opaca ou credenciais da Software Center.
 */
@Service
public class CAuthBffService {
    public static final String CHAVE_SESSAO_SC = "SC_BFF_SESSION_ID";
    public static final String CHAVE_CONTEXTO_SC = "SC_BFF_SESSION_CONTEXT";

    private final ISoftwareCenterClient softwareCenterClient;
    private final RSoftwareCenterProperties softwareCenterProperties;
    private final RSessaoCookieProperties sessaoCookieProperties;
    private final CsrfTokenRepository csrfTokenRepository;
    private final SessionRepository<? extends Session> sessionRepository;
    private final CookieSerializer sessionCookieSerializer;

    public CAuthBffService(
            ISoftwareCenterClient pSoftwareCenterClient,
            RSoftwareCenterProperties pSoftwareCenterProperties,
            RSessaoCookieProperties pSessaoCookieProperties,
            CsrfTokenRepository pCsrfTokenRepository,
            SessionRepository<? extends Session> pSessionRepository,
            CookieSerializer pSessionCookieSerializer
    ) {
        this.softwareCenterClient = pSoftwareCenterClient;
        this.softwareCenterProperties = pSoftwareCenterProperties;
        this.sessaoCookieProperties = pSessaoCookieProperties;
        this.csrfTokenRepository = pCsrfTokenRepository;
        this.sessionRepository = pSessionRepository;
        this.sessionCookieSerializer = pSessionCookieSerializer;
    }

    public RContextoSessaoBff login(RLoginBff pLogin, HttpServletRequest pRequest) {
        RSessaoCriada sessao = softwareCenterClient.criarSessaoComSenha(new RSoftwareCenterDtos.RCriarSessaoSenha(
                pLogin.email(),
                pLogin.senha(),
                resolverTenant(pLogin.tenantSubdominio())
        ));
        return criarSessaoLocal(pRequest, sessao);
    }

    public RContextoSessaoBff loginGoogle(RLoginGoogleBff pLogin, HttpServletRequest pRequest) {
        RSessaoCriada sessao = softwareCenterClient.criarSessaoComGoogle(new RSoftwareCenterDtos.RCriarSessaoGoogle(
                pLogin.credential(),
                resolverTenant(pLogin.tenantSubdominio())
        ));
        return criarSessaoLocal(pRequest, sessao);
    }

    public RContextoSessaoBff revalidarSessao(HttpSession pSessao) {
        String sessionId = obterSessionId(pSessao);
        RSessaoValidada sessao = softwareCenterClient.revalidarSessao(sessionId);
        RContextoSessaoBff contexto = converterContexto(sessao.context(), sessao.expiresAt());
        pSessao.setAttribute(CHAVE_CONTEXTO_SC, contexto);
        return contexto;
    }

    public RContextoSessaoBff obterContextoAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof RPrincipalSessaoBff principal) {
            return principal.contexto();
        }

        throw new CExceptionsSystem("Sessão não autenticada", HttpStatus.UNAUTHORIZED);
    }

    public void logout(HttpServletRequest pRequest, HttpServletResponse pResponse) {
        HttpSession sessao = pRequest.getSession(false);
        String sessionId = sessao == null ? null : obterSessionIdOpcional(sessao);

        invalidarSessaoLocal(sessao);
        removerCookieSessao(pResponse);
        csrfTokenRepository.saveToken(null, pRequest, pResponse);

        if (sessionId != null) {
            try {
                softwareCenterClient.revogarSessao(sessionId);
            } catch (RuntimeException pException) {
                // A sessão local já foi destruída e a revogação remota é somente melhor esforço.
            }
        }

        invalidarSessoesReferenciadasNoCookie(pRequest);
    }

    public void invalidarSessaoLocal(HttpSession pSessao) {
        if (pSessao != null) {
            String sessionId = pSessao.getId();
            // Remove o contexto antes da invalidação para não ressuscitar uma sessão em flush tardio.
            pSessao.removeAttribute(CHAVE_SESSAO_SC);
            pSessao.removeAttribute(CHAVE_CONTEXTO_SC);
            pSessao.setMaxInactiveInterval(0);
            pSessao.invalidate();
            sessionRepository.deleteById(sessionId);
        }
        SecurityContextHolder.clearContext();
    }

    public void cadastrar(RSolicitacaoAcesso pSolicitacao) {
        validarConfirmacaoSenha(pSolicitacao.senha(), pSolicitacao.confirmarSenha());
        validarSenhaSc(pSolicitacao.senha());
        softwareCenterClient.cadastrar(new RSoftwareCenterDtos.RCadastro(
                pSolicitacao.nome(),
                pSolicitacao.email(),
                pSolicitacao.senha()
        ));
    }

    public void solicitarRecuperacao(RSolicitacaoRecuperacaoSenha pSolicitacao) {
        softwareCenterClient.solicitarRecuperacao(new RSoftwareCenterDtos.RSolicitarRecuperacao(pSolicitacao.email()));
    }

    public void verificarRecuperacao(RVerificacaoCodigoRecuperacaoSenha pVerificacao) {
        softwareCenterClient.verificarRecuperacao(new RSoftwareCenterDtos.RVerificarRecuperacao(
                pVerificacao.email(), pVerificacao.codigo()
        ));
    }

    public void redefinirSenha(RRedefinicaoSenhaRecuperacao pRedefinicao) {
        validarConfirmacaoSenha(pRedefinicao.senha(), pRedefinicao.confirmarSenha());
        validarSenhaSc(pRedefinicao.senha());
        softwareCenterClient.redefinirSenha(new RSoftwareCenterDtos.RRedefinirSenha(
                pRedefinicao.email(), pRedefinicao.codigo(), pRedefinicao.senha()
        ));
    }

    private RContextoSessaoBff criarSessaoLocal(HttpServletRequest pRequest, RSessaoCriada pSessao) {
        if (pSessao == null || !StringUtils.hasText(pSessao.sessionId()) || pSessao.context() == null
                || pSessao.expiresAt() == null) {
            throw new CSoftwareCenterClientException(HttpStatus.BAD_GATEWAY, "Resposta de sessão inválida");
        }

        invalidarSessaoLocal(pRequest.getSession(false));
        RContextoSessaoBff contexto = converterContexto(pSessao.context(), pSessao.expiresAt());
        HttpSession sessao = pRequest.getSession(true);
        sessao.setAttribute(CHAVE_SESSAO_SC, pSessao.sessionId());
        sessao.setAttribute(CHAVE_CONTEXTO_SC, contexto);
        return contexto;
    }

    private RContextoSessaoBff converterContexto(RContextoSessao pContexto, java.time.Instant pExpiraEm) {
        if (pContexto == null || pExpiraEm == null || pContexto.userId() == null || pContexto.tenantId() == null
                || pContexto.membershipId() == null || pContexto.applicationId() == null || pContexto.roleId() == null
                || !StringUtils.hasText(pContexto.tenantSubdomain())) {
            throw new CSoftwareCenterClientException(HttpStatus.BAD_GATEWAY, "Resposta de sessão inválida");
        }

        return new RContextoSessaoBff(
                pContexto.userId(),
                pContexto.tenantId(),
                pContexto.tenantSubdomain(),
                pContexto.membershipId(),
                pContexto.applicationId(),
                pContexto.roleId(),
                pContexto.roleName(),
                pContexto.roleIcon(),
                pContexto.permissions(),
                pContexto.capabilities(),
                pExpiraEm
        );
    }

    private String resolverTenant(String pTenant) {
        String tenant = StringUtils.hasText(pTenant) ? pTenant.strip() : softwareCenterProperties.tenantDefault();
        if (!StringUtils.hasText(tenant)) {
            throw new CExceptionsSystem("O tenant deve ser informado", HttpStatus.BAD_REQUEST);
        }

        return tenant.strip();
    }

    private String obterSessionId(HttpSession pSessao) {
        String sessionId = obterSessionIdOpcional(pSessao);
        if (sessionId == null) {
            throw new CSoftwareCenterClientException(HttpStatus.UNAUTHORIZED, "Sessão inválida");
        }
        return sessionId;
    }

    private String obterSessionIdOpcional(HttpSession pSessao) {
        Object valor = pSessao.getAttribute(CHAVE_SESSAO_SC);
        return valor instanceof String sessionId && StringUtils.hasText(sessionId) ? sessionId : null;
    }

    private void validarConfirmacaoSenha(String pSenha, String pConfirmacao) {
        if (!StringUtils.hasText(pSenha) || !pSenha.equals(pConfirmacao)) {
            throw new CExceptionsSystem("A confirmação da senha não confere", HttpStatus.BAD_REQUEST);
        }
    }

    private void validarSenhaSc(String pSenha) {
        if (pSenha.length() < 10 || pSenha.length() > 72) {
            throw new CExceptionsSystem("A senha deve ter entre 10 e 72 caracteres", HttpStatus.BAD_REQUEST);
        }
    }

    private void removerCookieSessao(HttpServletResponse pResponse) {
        ResponseCookie cookie = ResponseCookie.from(sessaoCookieProperties.obterNome(), "")
                .path("/")
                .httpOnly(sessaoCookieProperties.usarHttpOnly())
                .secure(sessaoCookieProperties.usarSecure())
                .sameSite(sessaoCookieProperties.obterSameSite())
                .maxAge(0)
                .build();
        pResponse.addHeader("Set-Cookie", cookie.toString());
    }

    private void invalidarSessoesReferenciadasNoCookie(HttpServletRequest pRequest) {
        sessionCookieSerializer.readCookieValues(pRequest).forEach(sessionRepository::deleteById);
    }
}
