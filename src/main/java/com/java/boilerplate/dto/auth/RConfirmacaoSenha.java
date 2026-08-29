package com.java.boilerplate.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RConfirmacaoSenha(
        @Email(message = "Formato de e-mail inválido")
        String email,
        @NotBlank(message = "O campo senha é obrigatório")
        @Size(max = 72, message = "A senha nao pode exceder 72 caracteres")
        String password,
        @NotBlank(message = "A confirmação de senha é obrigatória")
        @Size(max = 72, message = "A confirmacao nao pode exceder 72 caracteres")
        String confirmPassword
) {
}
