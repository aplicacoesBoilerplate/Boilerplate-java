package com.java.boilerplate.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RRedefinicaoSenhaRecuperacao(
        @Email(message = "Formato de e-mail inválido")
        @NotBlank(message = "O campo e-mail é obrigatório")
        @Size(max = 150, message = "O e-mail nao pode exceder 150 caracteres")
        String email,
        @NotBlank(message = "O código de recuperação é obrigatório")
        @Pattern(regexp = "\\d{6}", message = "O codigo de recuperacao deve possuir seis digitos")
        String codigo,
        @NotBlank(message = "A nova senha é obrigatória")
        @Size(max = 72, message = "A senha nao pode exceder 72 caracteres")
        String senha,
        @NotBlank(message = "A confirmação da nova senha é obrigatória")
        @Size(max = 72, message = "A confirmacao nao pode exceder 72 caracteres")
        String confirmarSenha
) {
}
