package com.java.boilerplate.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record DTOOtp(
        @NotBlank(message = "O campo do código de verificação é obrigatório")
        String code,

        @NotBlank(message = "O campo de e-mail é obrigatório")
        String email,

        @NotBlank(message = "O campo de senha é obrigatório")
        String password
) {}