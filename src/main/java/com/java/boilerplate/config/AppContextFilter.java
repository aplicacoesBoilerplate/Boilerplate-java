package com.java.boilerplate.config;

import com.java.boilerplate.model.AppContext;
import com.java.boilerplate.service.context.AppContextService;
import com.java.boilerplate.service.context.CurrentAppContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AppContextFilter extends OncePerRequestFilter {
    private final AppContextService appContextService;
    private final CurrentAppContext currentAppContext;

    public AppContextFilter(AppContextService appContextService, CurrentAppContext currentAppContext) {
        this.appContextService = appContextService;
        this.currentAppContext = currentAppContext;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String headerContext = request.getHeader(AppContextService.HEADER_NAME);
            AppContext context = appContextService.resolve(headerContext);
            currentAppContext.set(context);
            filterChain.doFilter(request, response);
        } finally {
            currentAppContext.clear();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/doc") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/webjars");
    }
}
