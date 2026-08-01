package com.java.boilerplate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @description Configura o cookie HttpOnly que identifica a sessão persistida do BFF.
 */
@ConfigurationProperties(prefix = "server.servlet.session.cookie")
public record RSessaoCookieProperties(
        String name,
        Boolean httpOnly,
        Boolean secure,
        String sameSite
) {
    public String obterNome() {
        return name == null || name.isBlank() ? "BOILERPLATE_SESSION" : name;
    }

    public boolean usarHttpOnly() {
        return !Boolean.FALSE.equals(httpOnly);
    }

    public boolean usarSecure() {
        return Boolean.TRUE.equals(secure);
    }

    public String obterSameSite() {
        return sameSite == null || sameSite.isBlank() ? "Lax" : sameSite;
    }
}
