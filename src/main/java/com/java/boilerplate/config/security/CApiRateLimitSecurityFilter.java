package com.java.boilerplate.config.security;

import com.java.boilerplate.config.RRateLimitProperties;
import com.java.boilerplate.exception.CExceptionsSystem;
import com.java.boilerplate.model.CUsuario;
import com.java.boilerplate.service.helpers.CRateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.ArrayList;

/** Aplica uma única quota global e uma quota por sujeito a cada request da API. */
public class CApiRateLimitSecurityFilter extends OncePerRequestFilter {
    private final CRateLimitService rateLimitService;
    private final RRateLimitProperties properties;

    public CApiRateLimitSecurityFilter(CRateLimitService pRateLimitService, RRateLimitProperties pProperties) {
        this.rateLimitService = pRateLimitService;
        this.properties = pProperties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest pRequest) {
        String caminhoSemContexto = removerPrefixo(pRequest.getRequestURI(), pRequest.getContextPath());
        return !"/api/v1".equals(pRequest.getServletPath())
                && !caminhoSemContexto.startsWith("/api/v1/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest pRequest,
            HttpServletResponse pResponse,
            FilterChain pFilterChain
    ) throws ServletException, IOException {
        if (!healthLoopbackIsento(pRequest)) {
            try {
                Duration janela = Duration.ofSeconds(properties.windowSeconds());
                boolean autenticado = usuarioAutenticado() != null;
                List<CRateLimitService.RLimite> limites = new ArrayList<>(List.of(
                        new CRateLimitService.RLimite(
                                "api:global", "global", properties.globalRequestsPerWindow(), janela),
                        new CRateLimitService.RLimite(
                                "api:subject", sujeito(pRequest),
                                autenticado ? properties.authenticatedRequestsPerWindow() : properties.publicRequestsPerWindow(),
                                janela)
                ));
                if (documentacaoBasic(pRequest)) {
                    limites.add(new CRateLimitService.RLimite(
                            "documentacao-basic", pRequest.getRemoteAddr(), properties.loginAttemptsPerWindow(), janela));
                }
                rateLimitService.consumirTodos(limites);
            } catch (CExceptionsSystem pException) {
                escreverErro(pResponse, pException);
                return;
            }
        }
        pFilterChain.doFilter(pRequest, pResponse);
    }

    private CUsuario usuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof CUsuario usuario ? usuario : null;
    }

    private String sujeito(HttpServletRequest pRequest) {
        CUsuario usuario = usuarioAutenticado();
        return usuario == null ? "anonymous:" + pRequest.getRemoteAddr() : "user:" + usuario.getIdUsuario();
    }

    private boolean healthLoopbackIsento(HttpServletRequest pRequest) {
        boolean metodoLiveness = "GET".equalsIgnoreCase(pRequest.getMethod()) || "HEAD".equalsIgnoreCase(pRequest.getMethod());
        return metodoLiveness
                && "/actuator/health-check/public".equals(caminhoDaAplicacao(pRequest))
                && ("127.0.0.1".equals(pRequest.getRemoteAddr())
                || "::1".equals(pRequest.getRemoteAddr())
                || "0:0:0:0:0:0:0:1".equals(pRequest.getRemoteAddr()));
    }

    private boolean documentacaoBasic(HttpServletRequest pRequest) {
        String caminho = caminhoDaAplicacao(pRequest);
        String autorizacao = pRequest.getHeader("Authorization");
        return (caminho.equals("/doc") || caminho.startsWith("/doc/")
                || caminho.equals("/swagger-ui") || caminho.startsWith("/swagger-ui/")
                || caminho.equals("/v3/api-docs") || caminho.startsWith("/v3/api-docs/")
                || caminho.equals("/webjars") || caminho.startsWith("/webjars/"))
                && autorizacao != null
                && autorizacao.regionMatches(true, 0, "Basic ", 0, "Basic ".length());
    }

    private String caminhoDaAplicacao(HttpServletRequest pRequest) {
        String caminho = removerPrefixo(removerPrefixo(pRequest.getRequestURI(), pRequest.getContextPath()), pRequest.getServletPath());
        return caminho.startsWith("/api/v1/") ? caminho.substring("/api/v1".length()) : caminho;
    }

    private String removerPrefixo(String pCaminho, String pPrefixo) {
        return pPrefixo != null && !pPrefixo.isEmpty() && pCaminho.startsWith(pPrefixo)
                ? pCaminho.substring(pPrefixo.length()) : pCaminho;
    }

    private void escreverErro(HttpServletResponse pResponse, CExceptionsSystem pException) throws IOException {
        pResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        pResponse.setContentType("application/json");
        pResponse.setCharacterEncoding("UTF-8");
        pResponse.setHeader("Retry-After", String.valueOf(pException.getRetryAfterSeconds()));
        pResponse.getWriter().write("{\"mensagem\":\"" + pException.getMessage() + "\",\"status\":429}");
    }
}
