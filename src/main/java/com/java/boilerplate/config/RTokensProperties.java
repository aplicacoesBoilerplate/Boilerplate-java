package com.java.boilerplate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @description Record dos atributos do bloco 'tokens' no application.yml.
 */
@ConfigurationProperties(prefix = "tokens")
public record RTokensProperties(String secret, String issuer, Long accessTokenMinutes, String googleClientId) {}
