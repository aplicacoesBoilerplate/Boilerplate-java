package com.java.boilerplate.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RLoginGoogle(
        @NotBlank(message = "A credencial do Google é obrigatória")
        @Size(max = 8192, message = "A credencial do Google excede o limite permitido")
        String credential
) {
}
