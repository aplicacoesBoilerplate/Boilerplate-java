package com.java.boilerplate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @description Record dos atributos do bloco 'app' no application.yml.
 */
@ConfigurationProperties(prefix = "app")
public record RAppProperties(String frontendUrl, String corsAllowedOrigins, Boolean exposeErrorTrace) {
    public boolean deveExporTraceErro() {
        return Boolean.TRUE.equals(exposeErrorTrace);
    }
}
