package com.java.boilerplate.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RVerificacaoCodigoRecuperacaoSenha(
        @Email(message = "Formato de e-mail inválido")
        @NotBlank(message = "O campo e-mail é obrigatório")
        String email,
        @NotBlank(message = "O código de recuperação é obrigatório")
        String codigo
) {
}
