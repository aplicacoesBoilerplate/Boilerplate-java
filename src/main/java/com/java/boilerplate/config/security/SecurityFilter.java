package com.java.boilerplate.config.security;

import com.java.boilerplate.repository.IUsersRepository;
import com.java.boilerplate.service.context.AppContextService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class SecurityFilter extends OncePerRequestFilter {
    private final TokenService tokenService;
    private final IUsersRepository userRepository;
    private final AppContextService appContextService;

    public SecurityFilter(TokenService tokenService, @Lazy IUsersRepository userRepository, AppContextService appContextService) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.appContextService = appContextService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var token = this.recoverToken(request);
        if(token != null) {
            var subject = tokenService.validateToken(token);
            var tokenContextKey = tokenService.getContextKey(token);
            UserDetails user = null;

            if (!subject.isBlank() && appContextService.getCurrentKey().equals(tokenContextKey)) {
                user = userRepository.findByUsernameOrEmailAndContextKey(subject, tokenContextKey);
            }

            if (user != null) {
                var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/doc") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/webjars");
    }

    private String recoverToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if(authHeader == null) return null;
        return authHeader.replace("Bearer ", "");
    }
}
