package com.java.boilerplate.dto.auth;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

/**
 * @description Credential pública do Google encaminhada pelo BFF para validação centralizada.
 */
public record RLoginGoogleBff(
        @NotBlank(message = "A credencial do Google é obrigatória")
        String credential,

        @JsonAlias({"tenantSubdomain", "tenant"}) String tenantSubdominio) {}
