package com.java.boilerplate.config.security;

import com.java.boilerplate.dto.auth.RContextoSessaoBff;
import com.java.boilerplate.integration.softwarecenter.CSoftwareCenterClientException;
import com.java.boilerplate.service.CAuthBffService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * @description Revalida a sessão opaca na Software Center antes de disponibilizar o contexto BFF em rotas protegidas.
 */
public class CSessaoBffFilter extends OncePerRequestFilter {
    private static final Set<String> ROTAS_SEM_REVALIDACAO = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/login/google",
            "/api/v1/auth/csrf",
            "/api/v1/auth/logout",
            "/api/v1/auth/cadastro",
            "/api/v1/auth/solicitacoes-acesso",
            "/api/v1/auth/recuperacao-senha/solicitar",
            "/api/v1/auth/recuperacao-senha/verificar",
            "/api/v1/auth/recuperacao-senha/redefinir",
            "/actuator/health-check",
            "/error");

    private final CAuthBffService authBffService;

    public CSessaoBffFilter(CAuthBffService pAuthBffService) {
        this.authBffService = pAuthBffService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest pRequest) {
        return "OPTIONS".equalsIgnoreCase(pRequest.getMethod())
                || ROTAS_SEM_REVALIDACAO.contains(pRequest.getServletPath());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest pRequest, HttpServletResponse pResponse, FilterChain pFilterChain)
            throws ServletException, IOException {
        HttpSession sessao = pRequest.getSession(false);
        if (sessao == null) {
            pFilterChain.doFilter(pRequest, pResponse);
            return;
        }

        try {
            RContextoSessaoBff contexto = authBffService.revalidarSessao(sessao);
            RPrincipalSessaoBff principal = new RPrincipalSessaoBff(contexto);
            List<SimpleGrantedAuthority> authorities = contexto.permissions().stream()
                    .map(pPermissao -> new SimpleGrantedAuthority("PERMISSION_" + pPermissao))
                    .toList();
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            pFilterChain.doFilter(pRequest, pResponse);
        } catch (CSoftwareCenterClientException pException) {
            if (pException.deveInvalidarSessaoLocal()) {
                authBffService.invalidarSessaoLocal(sessao);
            } else {
                SecurityContextHolder.clearContext();
            }
            pResponse.sendError(pException.getStatus().value());
        }
    }
}
