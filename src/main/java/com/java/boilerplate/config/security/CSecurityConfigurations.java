package com.java.boilerplate.config.security;

import com.java.boilerplate.config.RAcessoSwagger;
import com.java.boilerplate.config.RDocumentacaoProperties;
import com.java.boilerplate.service.CAuthBffService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
public class CSecurityConfigurations {
    private final RDocumentacaoProperties documentacaoProperties;
    private final CAutorizacaoRbacManager autorizacaoRbacManager;

    public CSecurityConfigurations(
            RDocumentacaoProperties pDocumentacaoProperties, CAutorizacaoRbacManager pAutorizacaoRbacManager) {
        this.documentacaoProperties = pDocumentacaoProperties;
        this.autorizacaoRbacManager = pAutorizacaoRbacManager;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain swaggerSecurityFilterChain(HttpSecurity pHttp, PasswordEncoder pPasswordEncoder)
            throws Exception {
        RAcessoSwagger usuarioDoc = resolveAcessoDocumentacao(pPasswordEncoder);
        UserDetails admin = User.builder()
                .username(usuarioDoc.usuario())
                .password(usuarioDoc.senha())
                .roles("SWAGGER_ADMIN")
                .build();

        return pHttp.securityMatchers(pMatchers -> pMatchers.requestMatchers(
                        "/doc", "/doc/**", "/swagger-ui/**", "/v3/api-docs/**", "/webjars/**"))
                .cors(Customizer.withDefaults())
                .csrf(pCsrf -> pCsrf.disable())
                .sessionManagement(pSession -> pSession.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(pAuth -> pAuth.anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .exceptionHandling(pEx -> pEx.authenticationEntryPoint((pRequest, pResponse, pAuthException) -> {
                    pResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    pResponse.setHeader("WWW-Authenticate", "Basic realm=\"Swagger\"");
                }))
                .userDetailsService(new InMemoryUserDetailsManager(admin))
                .build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(
            HttpSecurity pHttp, CookieCsrfTokenRepository pCsrfTokenRepository, CAuthBffService pAuthBffService)
            throws Exception {
        return pHttp.cors(Customizer.withDefaults())
                .csrf(pCsrf -> pCsrf.csrfTokenRepository(pCsrfTokenRepository)
                        .csrfTokenRequestHandler(new CSpaCsrfTokenRequestHandler()))
                .sessionManagement(pSession -> pSession.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .exceptionHandling(pExceptions -> pExceptions
                        .authenticationEntryPoint((pRequest, pResponse, pException) ->
                                pResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                        .accessDeniedHandler((pRequest, pResponse, pException) ->
                                pResponse.sendError(HttpServletResponse.SC_FORBIDDEN)))
                .authorizeHttpRequests(pAuthorize -> pAuthorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**")
                        .permitAll()
                        .requestMatchers("/actuator/health-check", "/error")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/csrf")
                        .permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/auth/login",
                                "/api/v1/auth/login/google",
                                "/api/v1/auth/cadastro",
                                "/api/v1/auth/solicitacoes-acesso",
                                "/api/v1/auth/recuperacao-senha/solicitar",
                                "/api/v1/auth/recuperacao-senha/verificar",
                                "/api/v1/auth/recuperacao-senha/redefinir",
                                "/api/v1/auth/logout")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/session")
                        .authenticated()
                        .anyRequest()
                        .access(autorizacaoRbacManager))
                .addFilterBefore(new CSessaoBffFilter(pAuthBffService), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration pAuthenticationConfiguration)
            throws Exception {
        return pAuthenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * @description Função que resolve o acesso à documentação com o swagger.
     */
    private RAcessoSwagger resolveAcessoDocumentacao(PasswordEncoder pPasswordEncoder) {
        if (documentacaoProperties.usuario() == null
                || documentacaoProperties.usuario().isBlank()) {
            throw new IllegalStateException("A propriedade documentacao.usuario deve ser informada");
        }

        if (documentacaoProperties.senhaHash() != null
                && !documentacaoProperties.senhaHash().isBlank()) {
            return new RAcessoSwagger(
                    documentacaoProperties.usuario(),
                    documentacaoProperties.senhaHash().replaceFirst("^\\{bcrypt}", ""));
        }

        if (documentacaoProperties.senha() == null
                || documentacaoProperties.senha().isBlank()) {
            throw new IllegalStateException(
                    "A propriedade documentacao.senha ou documentacao.senhaHash deve ser informada");
        }

        return new RAcessoSwagger(
                documentacaoProperties.usuario(), pPasswordEncoder.encode(documentacaoProperties.senha()));
    }
}
