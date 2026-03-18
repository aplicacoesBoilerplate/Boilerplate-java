package com.java.boilerplate.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DTOAuth(
        @NotBlank(message = "O campo de e-mail ou nome de usuário é obrigatório")
        String usernameOrEmail,

        @NotBlank(message = "O campo de senha é obrigatório")
        @Size(min = 8, max = 20, message = "A senha deve conter entre 8 a 20 caracteres")
        String password
) {}
