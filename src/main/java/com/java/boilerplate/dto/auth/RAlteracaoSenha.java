package com.java.boilerplate.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RAlteracaoSenha(
        @Email(message = "Formato de e-mail inválido")
        String emailUser,
        @NotBlank(message = "A senha atual é obrigatória")
        @Size(max = 72, message = "A senha atual nao pode exceder 72 caracteres")
        String passwordUser,
        @NotBlank(message = "A nova senha é obrigatória")
        @Size(max = 72, message = "A nova senha nao pode exceder 72 caracteres")
        String newPassword,
        @NotBlank(message = "A confirmação da nova senha é obrigatória")
        @Size(max = 72, message = "A confirmacao nao pode exceder 72 caracteres")
        String confirmNewPassword
) {
}
