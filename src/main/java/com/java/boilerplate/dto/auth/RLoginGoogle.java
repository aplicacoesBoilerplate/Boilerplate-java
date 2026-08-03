package com.java.boilerplate.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record RLoginGoogle(
        @NotBlank(message = "A credencial do Google é obrigatória")
        String credential) {}
