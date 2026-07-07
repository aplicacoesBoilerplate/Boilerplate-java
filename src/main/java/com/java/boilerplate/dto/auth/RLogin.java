package com.java.boilerplate.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RLogin(
        @Email(message = "Formato de e-mail inválido")
        @NotBlank(message = "O campo e-mail é obrigatório")
        String email,

        @NotBlank(message = "O campo senha é obrigatório")
        String password
) {
}
