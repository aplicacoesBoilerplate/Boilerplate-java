package com.java.boilerplate.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RAlteracaoSenha(
        @Email(message = "Formato de e-mail inválido")
        String emailUser,
        @NotBlank(message = "A senha atual é obrigatória")
        String passwordUser,
        @NotBlank(message = "A nova senha é obrigatória")
        String newPassword,
        @NotBlank(message = "A confirmação da nova senha é obrigatória")
        String confirmNewPassword
) {
}
