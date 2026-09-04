package com.java.boilerplate.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "security.rate-limit")
public record RRateLimitProperties(
        @Min(10) @Max(3600) Integer windowSeconds,
        @Min(1) @Max(1_000_000) Integer globalRequestsPerWindow,
        @Min(1) @Max(100_000) Integer authenticatedRequestsPerWindow,
        @Min(1) @Max(100) Integer publicRequestsPerWindow,
        @Min(1) @Max(20) Integer loginAttemptsPerWindow,
        @Min(100) @Max(100_000) Integer maxTrackedKeys
) {
}
