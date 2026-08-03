package com.java.boilerplate.config.security;

import com.java.boilerplate.model.CUsuario;
import com.java.boilerplate.repository.IUsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class CSecurityFilter extends OncePerRequestFilter {
    private final CTokenService tokenService;
    private final IUsuarioRepository usuarioRepository;

    public CSecurityFilter(CTokenService pTokenService, IUsuarioRepository pUsuarioRepository) {
        this.tokenService = pTokenService;
        this.usuarioRepository = pUsuarioRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest pRequest, HttpServletResponse pResponse, FilterChain pFilterChain)
            throws ServletException, IOException {
        String token = recuperarToken(pRequest);
        if (token != null) {
            String email = tokenService.validarToken(token);
            CUsuario usuario = email.isBlank()
                    ? null
                    : usuarioRepository.findByEmailIgnoreCase(email).orElse(null);

            if (usuario != null && usuario.getAtivo()) {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        pFilterChain.doFilter(pRequest, pResponse);
    }

    private String recuperarToken(HttpServletRequest pRequest) {
        String authHeader = pRequest.getHeader("Authorization");
        if (authHeader == null || authHeader.isBlank()) {
            return null;
        }
        return authHeader.replace("Bearer ", "").trim();
    }
}
