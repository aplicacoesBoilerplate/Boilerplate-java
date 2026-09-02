package com.java.boilerplate.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "redis")
public record RRedisProperties(@NotNull Boolean enabled) {}
