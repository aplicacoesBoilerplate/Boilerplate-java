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
        return !pRequest.getRequestURI().startsWith("/api/v1/");
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
                rateLimitService.consumirTodos(List.of(
                        new CRateLimitService.RLimite(
                                "api:global", "global", properties.globalRequestsPerWindow(), janela),
                        new CRateLimitService.RLimite(
                                "api:subject", sujeito(pRequest),
                                autenticado ? properties.authenticatedRequestsPerWindow() : properties.publicRequestsPerWindow(),
                                janela)
                ));
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
                && "/api/v1/actuator/health-check/public".equals(pRequest.getRequestURI())
                && ("127.0.0.1".equals(pRequest.getRemoteAddr())
                || "::1".equals(pRequest.getRemoteAddr())
                || "0:0:0:0:0:0:0:1".equals(pRequest.getRemoteAddr()));
    }

    private void escreverErro(HttpServletResponse pResponse, CExceptionsSystem pException) throws IOException {
        pResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        pResponse.setContentType("application/json");
        pResponse.setCharacterEncoding("UTF-8");
        pResponse.setHeader("Retry-After", String.valueOf(pException.getRetryAfterSeconds()));
        pResponse.getWriter().write("{\"mensagem\":\"" + pException.getMessage() + "\",\"status\":429}");
    }
}
