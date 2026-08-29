package com.java.boilerplate.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @description Record dos atributos do bloco 'app' no application.yml.
 */
@ConfigurationProperties(prefix = "app")
@Validated
public record RAppProperties(
        String frontendUrl,
        String corsAllowedOrigins,
        Boolean exposeErrorTrace,
        @Min(100) @Max(100_000) Integer maxPersistedErrors
) {
    public boolean deveExporTraceErro() {
        return Boolean.TRUE.equals(exposeErrorTrace);
    }

    public int limiteErrosPersistidos() {
        return maxPersistedErrors == null ? 1000 : maxPersistedErrors;
    }
}
