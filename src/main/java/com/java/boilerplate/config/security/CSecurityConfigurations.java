package com.java.boilerplate.config.security;

import com.java.boilerplate.config.RAcessoSwagger;
import com.java.boilerplate.config.RDocumentacaoProperties;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.withDefaults;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class CSecurityConfigurations {
    private final CSecurityFilter securityFilter;
    private final RDocumentacaoProperties documentacaoProperties;
    private final CAutorizacaoRbacManager autorizacaoRbacManager;

    public CSecurityConfigurations(
            CSecurityFilter pSecurityFilter,
            RDocumentacaoProperties pDocumentacaoProperties,
            CAutorizacaoRbacManager pAutorizacaoRbacManager
    ) {
        this.securityFilter = pSecurityFilter;
        this.documentacaoProperties = pDocumentacaoProperties;
        this.autorizacaoRbacManager = pAutorizacaoRbacManager;
    }

    @Bean
    @Order(1)
    @ConditionalOnProperty(prefix = "documentacao", name = "enabled", havingValue = "true")
    public SecurityFilterChain swaggerSecurityFilterChain(
            HttpSecurity pHttp,
            PasswordEncoder pPasswordEncoder,
            CApiRateLimitSecurityFilter pApiRateLimitSecurityFilter
    ) throws Exception {
        RAcessoSwagger usuarioDoc = resolveAcessoDocumentacao(pPasswordEncoder);
        PathPatternRequestMatcher.Builder api = withDefaults().basePath("/api/v1");
        UserDetails admin = User.builder()
                .username(usuarioDoc.usuario())
                .password(usuarioDoc.senha())
                .roles("SWAGGER_ADMIN")
                .build();

        return pHttp
                .securityMatchers(pMatchers -> pMatchers.requestMatchers(
                        api.matcher("/doc"),
                        api.matcher("/doc/**"),
                        api.matcher("/swagger-ui/**"),
                        api.matcher("/v3/api-docs/**"),
                        api.matcher("/webjars/**")
                ))
                .cors(Customizer.withDefaults())
                .csrf(pCsrf -> pCsrf.disable())
                .sessionManagement(pSession -> pSession.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(pAuth -> pAuth.anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .addFilterBefore(pApiRateLimitSecurityFilter, BasicAuthenticationFilter.class)
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
            HttpSecurity pHttp,
            CApiRateLimitSecurityFilter pApiRateLimitSecurityFilter
    ) throws Exception {
        PathPatternRequestMatcher.Builder api = withDefaults().basePath("/api/v1");
        return pHttp
                .cors(Customizer.withDefaults())
                .csrf(pCsrf -> pCsrf.disable())
                .sessionManagement(pSession -> pSession.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(pExceptions -> pExceptions.authenticationEntryPoint((pRequest, pResponse, pException) -> {
                    pResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    pResponse.setHeader("WWW-Authenticate", "Bearer");
                }))
                .authorizeHttpRequests(pAuthorize -> pAuthorize
                        .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
                        .requestMatchers(api.matcher(HttpMethod.GET, "/actuator/health-check/public")).permitAll()
                        .requestMatchers(api.matcher(HttpMethod.HEAD, "/actuator/health-check/public")).permitAll()
                        .requestMatchers(api.matcher("/actuator/health-check/public")).denyAll()
                        .requestMatchers(api.matcher("/actuator/health-check"), api.matcher("/actuator/health-check/**")).hasRole("ADMIN")
                        .requestMatchers(api.matcher("/actuator/metrics"), api.matcher("/actuator/metrics/**")).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(api.matcher(HttpMethod.POST, "/auth/login"), api.matcher(HttpMethod.POST, "/auth/login/google")).permitAll()
                        .requestMatchers(api.matcher(HttpMethod.POST, "/auth/token/login"), api.matcher(HttpMethod.POST, "/auth/token/login/google")).permitAll()
                        .requestMatchers(api.matcher(HttpMethod.POST, "/auth/cadastro"), api.matcher(HttpMethod.POST, "/auth/solicitacoes-acesso")).permitAll()
                        .requestMatchers(api.matcher(HttpMethod.POST, "/auth/recuperacao-senha/**")).permitAll()
                        .requestMatchers(api.matcher(HttpMethod.GET, "/auth/me"), api.matcher(HttpMethod.GET, "/auth/me/cargo")).authenticated()
                        .requestMatchers(api.matcher(HttpMethod.GET, "/rbac/manifesto")).authenticated()
                        .requestMatchers(api.matcher(HttpMethod.POST, "/auth/logout")).authenticated()
                        .requestMatchers(api.matcher(HttpMethod.PUT, "/auth/senha")).authenticated()
                        .requestMatchers(api.matcher(HttpMethod.POST, "/auth/senha/confirmar")).authenticated()
                        .requestMatchers(api.matcher(HttpMethod.GET, "/preferencias/**")).authenticated()
                        .requestMatchers(api.matcher(HttpMethod.PUT, "/preferencias/**")).authenticated()
                        .requestMatchers(api.matcher(HttpMethod.DELETE, "/preferencias/**")).authenticated()
                        .anyRequest().access(autorizacaoRbacManager)
                )
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(pApiRateLimitSecurityFilter, CSecurityFilter.class)
                .build();
    }

    @Bean
    public CApiRateLimitSecurityFilter apiRateLimitSecurityFilter(
            com.java.boilerplate.service.helpers.CRateLimitService pRateLimitService,
            com.java.boilerplate.config.RRateLimitProperties pRateLimitProperties
    ) {
        return new CApiRateLimitSecurityFilter(pRateLimitService, pRateLimitProperties);
    }

    @Bean
    public FilterRegistrationBean<CApiRateLimitSecurityFilter> apiRateLimitSecurityFilterRegistration(
            CApiRateLimitSecurityFilter pFilter
    ) {
        FilterRegistrationBean<CApiRateLimitSecurityFilter> registration = new FilterRegistrationBean<>(pFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration pAuthenticationConfiguration) throws Exception {
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
        if (documentacaoProperties.usuario() == null || documentacaoProperties.usuario().isBlank()) {
            throw new IllegalStateException("A propriedade documentacao.usuario deve ser informada");
        }

        if (documentacaoProperties.senhaHash() != null && !documentacaoProperties.senhaHash().isBlank()) {
            return new RAcessoSwagger(documentacaoProperties.usuario(), documentacaoProperties.senhaHash().replaceFirst("^\\{bcrypt}", ""));
        }

        if (documentacaoProperties.senha() == null || documentacaoProperties.senha().isBlank()) {
            throw new IllegalStateException("A propriedade documentacao.senha ou documentacao.senhaHash deve ser informada");
        }

        return new RAcessoSwagger(documentacaoProperties.usuario(), pPasswordEncoder.encode(documentacaoProperties.senha()));
    }
}
