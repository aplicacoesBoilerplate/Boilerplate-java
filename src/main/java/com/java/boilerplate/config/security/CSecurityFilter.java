package com.java.boilerplate.config.security;

import com.java.boilerplate.model.CUsuario;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class CSecurityFilter extends OncePerRequestFilter {
    private final CTokenService tokenService;

    public CSecurityFilter(CTokenService pTokenService) {
        this.tokenService = pTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest pRequest, HttpServletResponse pResponse, FilterChain pFilterChain) throws ServletException, IOException {
        String token = recuperarToken(pRequest);
        if (token != null) {
            CUsuario usuario = tokenService.validarToken(token);

            if (usuario != null && usuario.isEnabled()) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        pFilterChain.doFilter(pRequest, pResponse);
    }

    private String recuperarToken(HttpServletRequest pRequest) {
        String authHeader = pRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7).trim();
        return token.isBlank() ? null : token;
    }
}
