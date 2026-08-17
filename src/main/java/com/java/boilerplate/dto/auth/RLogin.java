package com.java.boilerplate.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RLogin(
        @Email(message = "Formato de e-mail inválido")
        @NotBlank(message = "O campo identificação de acesso é obrigatório")
        @Size(max = 150, message = "A identificacao de acesso nao pode exceder 150 caracteres")
        String identificacaoAcesso,

        @NotBlank(message = "O campo senha é obrigatório")
        @Size(max = 72, message = "A senha nao pode exceder 72 caracteres")
        String senha
) {
}
