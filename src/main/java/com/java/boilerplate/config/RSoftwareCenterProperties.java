package com.java.boilerplate.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @description Configura o canal servidor-servidor entre o BFF e a Software Center.
 */
@ConfigurationProperties(prefix = "software-center")
public record RSoftwareCenterProperties(
        String baseUrl, String bffClientId, String bffClientSecret, String tenantDefault, Duration timeout) {
    public Duration obterTimeout() {
        return timeout == null || timeout.isNegative() || timeout.isZero() ? Duration.ofSeconds(5) : timeout;
    }
}
