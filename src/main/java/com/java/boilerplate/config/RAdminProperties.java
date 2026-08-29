package com.java.boilerplate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bootstrap.admin")
public record RAdminProperties(
        Boolean enabled,
        String name,
        String email,
        String password
) {
    public boolean habilitado() {
        return Boolean.TRUE.equals(enabled);
    }
}
