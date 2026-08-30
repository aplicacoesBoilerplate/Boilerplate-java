package com.java.boilerplate.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * @description Classe de configuração para o CORS, necessária para políticas de browsers quando uma API é consumida por um frontend.
 * @property {RAppProperties} appProperties - Acesso aos atributos que o record carregou.
 */
@Configuration
public class CCorsConfiguration {
    private final RAppProperties appProperties;

    public CCorsConfiguration(RAppProperties pAppProperties) {
        this.appProperties = pAppProperties;
    }

    /**
     * @description Bean de configuração para definição das políticas de CORS desta aplicação.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration =
                new org.springframework.web.cors.CorsConfiguration();
        configuration.setAllowedOrigins(this.obterOrigensPermitidas());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Accept-Language", "X-XSRF-TOKEN"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * @description Método privado para obter uma listagem com as origens que podem requisitar à API.
     * Usa o atributo corsAllowedOrigins que veio de RAppProperties.
     */
    private List<String> obterOrigensPermitidas() {
        return Arrays.stream(appProperties.corsAllowedOrigins().split(","))
                .map(String::trim)
                .filter(pOrigem -> !pOrigem.isBlank())
                .toList();
    }
}
