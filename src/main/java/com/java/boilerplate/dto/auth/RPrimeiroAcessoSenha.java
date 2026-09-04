package com.java.boilerplate.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record RPrimeiroAcessoSenha(
        @NotBlank(message = "O token de ativação é obrigatório") String token,
        @NotBlank(message = "A senha é obrigatória") String senha,
        @NotBlank(message = "A confirmação da senha é obrigatória") String confirmarSenha
) {
}
