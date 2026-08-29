package com.java.boilerplate.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RSolicitacaoRecuperacaoSenha(
        @Email(message = "Formato de e-mail inválido")
        @NotBlank(message = "O campo e-mail é obrigatório")
        @Size(max = 150, message = "O e-mail nao pode exceder 150 caracteres")
        String email
) {
}
