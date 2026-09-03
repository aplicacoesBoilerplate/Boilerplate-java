package com.java.boilerplate.config.security;

import com.java.boilerplate.config.RRateLimitProperties;
import com.java.boilerplate.service.helpers.CRateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Set;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class CPublicRequestSecurityFilter extends OncePerRequestFilter {
    private static final int MAX_PUBLIC_BODY_BYTES = 64 * 1024;
    private static final Set<String> PUBLIC_POST_PATHS = Set.of(
            "/auth/login",
            "/auth/login/google",
            "/auth/recuperacao-senha/solicitar",
            "/auth/recuperacao-senha/verificar",
            "/auth/recuperacao-senha/redefinir"
    );
    private static final Set<String> DOCUMENTATION_PREFIXES = Set.of(
            "/doc", "/swagger-ui", "/v3/api-docs", "/webjars"
    );

    private final CRateLimitService rateLimitService;
    private final RRateLimitProperties properties;

    public CPublicRequestSecurityFilter(CRateLimitService pRateLimitService, RRateLimitProperties pProperties) {
        this.rateLimitService = pRateLimitService;
        this.properties = pProperties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest pRequest,
            HttpServletResponse pResponse,
            FilterChain pFilterChain
    ) throws ServletException, IOException {
        String caminho = caminhoDaAplicacao(pRequest);
        boolean probeHealthPublico = ("GET".equalsIgnoreCase(pRequest.getMethod())
                || "HEAD".equalsIgnoreCase(pRequest.getMethod()))
                && "/actuator/health-check/public".equals(caminho);
        boolean rotaPublica = ("POST".equalsIgnoreCase(pRequest.getMethod())
                && PUBLIC_POST_PATHS.contains(caminho))
                || probeHealthPublico;
        boolean autenticacaoDocumentacao = DOCUMENTATION_PREFIXES.stream()
                .anyMatch(pPrefixo -> caminho.equals(pPrefixo)
                        || caminho.startsWith(pPrefixo + "/"))
                && pRequest.getHeader("Authorization") != null
                && pRequest.getHeader("Authorization").startsWith("Basic ");
        boolean metodoComCorpo = Set.of("POST", "PUT", "PATCH").contains(pRequest.getMethod().toUpperCase());
        if (!rotaPublica && !autenticacaoDocumentacao && !metodoComCorpo) {
            pFilterChain.doFilter(pRequest, pResponse);
            return;
        }

        if (!metodoComCorpo) {
            pFilterChain.doFilter(pRequest, pResponse);
            return;
        }

        long tamanhoDeclarado = pRequest.getContentLengthLong();
        if (tamanhoDeclarado > MAX_PUBLIC_BODY_BYTES) {
            escreverErro(pResponse, HttpStatus.CONTENT_TOO_LARGE, "Corpo da requisicao excede o limite permitido", null);
            return;
        }
        byte[] body = pRequest.getInputStream().readNBytes(MAX_PUBLIC_BODY_BYTES + 1);
        if (body.length > MAX_PUBLIC_BODY_BYTES) {
            escreverErro(pResponse, HttpStatus.CONTENT_TOO_LARGE, "Corpo da requisicao excede o limite permitido", null);
            return;
        }

        pFilterChain.doFilter(new CBodyRequestWrapper(pRequest, body), pResponse);
    }

    private String caminhoDaAplicacao(HttpServletRequest pRequest) {
        String caminho = pRequest.getRequestURI();
        String contextPath = pRequest.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && caminho.startsWith(contextPath)) {
            caminho = caminho.substring(contextPath.length());
        }

        String servletPath = pRequest.getServletPath();
        if (servletPath != null && !servletPath.isEmpty() && caminho.startsWith(servletPath)) {
            caminho = caminho.substring(servletPath.length());
        }

        return caminho.startsWith("/api/v1/") ? caminho.substring("/api/v1".length()) : caminho;
    }

    private void escreverErro(HttpServletResponse pResponse, HttpStatus pStatus, String pMensagem, Integer pRetryAfter) throws IOException {
        pResponse.setStatus(pStatus.value());
        pResponse.setContentType("application/json");
        pResponse.setCharacterEncoding("UTF-8");
        pResponse.getWriter().write("{\"mensagem\":\"" + pMensagem + "\",\"status\":" + pStatus.value() + "}");
    }

    private static final class CBodyRequestWrapper extends HttpServletRequestWrapper {
        private final byte[] body;

        private CBodyRequestWrapper(HttpServletRequest pRequest, byte[] pBody) {
            super(pRequest);
            this.body = pBody;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return input.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener pReadListener) {
                    // Leitura sincrona usada pelo stack MVC.
                }

                @Override
                public int read() {
                    return input.read();
                }
            };
        }

        @Override
        public int getContentLength() {
            return body.length;
        }

        @Override
        public long getContentLengthLong() {
            return body.length;
        }
    }
}
