package com.java.boilerplate.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RSolicitacaoAcesso(
        @NotBlank(message = "O campo nome é obrigatório")
        @Size(max = 120, message = "O nome nao pode exceder 120 caracteres")
        String nome,
        @Email(message = "Formato de e-mail inválido")
        @NotBlank(message = "O campo e-mail é obrigatório")
        @Size(max = 150, message = "O e-mail nao pode exceder 150 caracteres")
        String email,
        @NotBlank(message = "O campo senha é obrigatório")
        @Size(max = 72, message = "A senha nao pode exceder 72 caracteres")
        String senha,
        @NotBlank(message = "A confirmação de senha é obrigatória")
        @Size(max = 72, message = "A confirmacao nao pode exceder 72 caracteres")
        String confirmarSenha
) {
}
