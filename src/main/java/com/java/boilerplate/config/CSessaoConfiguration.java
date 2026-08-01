package com.java.boilerplate.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * @description Garante que o Spring Session emita o cookie principal com as propriedades do BFF.
 */
@Configuration
@EnableJdbcHttpSession
public class CSessaoConfiguration {
    @Bean
    public CookieSerializer springSessionCookieSerializer(RSessaoCookieProperties pProperties) {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName(pProperties.obterNome());
        serializer.setCookiePath("/");
        serializer.setUseHttpOnlyCookie(pProperties.usarHttpOnly());
        serializer.setUseSecureCookie(pProperties.usarSecure());
        serializer.setSameSite(pProperties.obterSameSite());
        return serializer;
    }

    @Bean
    public CookieCsrfTokenRepository csrfTokenRepository(RSessaoCookieProperties pProperties) {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieName("BOILERPLATE-XSRF-TOKEN");
        repository.setHeaderName("X-XSRF-TOKEN");
        repository.setCookiePath("/");
        repository.setCookieCustomizer(pCookie -> pCookie
                .path("/")
                .secure(pProperties.usarSecure())
                .sameSite(pProperties.obterSameSite()));
        return repository;
    }
}
