package com.java.boilerplate.config.security;

import com.java.boilerplate.config.RAcessoSwagger;
import com.java.boilerplate.config.RDocumentacaoProperties;
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

@Configuration
@EnableWebSecurity
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
    public SecurityFilterChain swaggerSecurityFilterChain(HttpSecurity pHttp, PasswordEncoder pPasswordEncoder) throws Exception {
        RAcessoSwagger usuarioDoc = resolveAcessoDocumentacao(pPasswordEncoder);
        UserDetails admin = User.builder()
                .username(usuarioDoc.usuario())
                .password(usuarioDoc.senha())
                .roles("SWAGGER_ADMIN")
                .build();

        return pHttp
                .securityMatchers(pMatchers -> pMatchers.requestMatchers("/doc", "/doc/**", "/swagger-ui/**", "/v3/api-docs/**", "/webjars/**"))
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
    public SecurityFilterChain securityFilterChain(HttpSecurity pHttp) throws Exception {
        return pHttp
                .cors(Customizer.withDefaults())
                .csrf(pCsrf -> pCsrf.disable())
                .sessionManagement(pSession -> pSession.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(pAuthorize -> pAuthorize
                        .requestMatchers("/actuator/health-check").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/login/google").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/solicitacoes-acesso").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/recuperacao-senha/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/auth/me", "/auth/me/cargo").authenticated()
                        .requestMatchers(HttpMethod.POST, "/auth/logout").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/auth/senha").authenticated()
                        .requestMatchers(HttpMethod.POST, "/auth/senha/confirmar").authenticated()
                        .requestMatchers(HttpMethod.GET, "/preferencias/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/preferencias/**").authenticated()
                        .anyRequest().access(autorizacaoRbacManager)
                )
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
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
