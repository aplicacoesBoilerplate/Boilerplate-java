package com.java.boilerplate.dto.auth;

/**
 * @description Token CSRF de curta duração que a SPA envia no header das requisições mutáveis.
 */
public record RRespostaCsrfBff(
        String token,
        String headerName,
        String parameterName
) {
}
