package com.java.boilerplate.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RConfirmacaoSenha(
        @Email(message = "Formato de e-mail inválido")
        String email,
        @NotBlank(message = "O campo senha é obrigatório")
        String password,
        @NotBlank(message = "A confirmação de senha é obrigatória")
        String confirmPassword
) {
}
