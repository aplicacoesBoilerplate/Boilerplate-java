package com.java.boilerplate.dto.auth;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * @description Credenciais encaminhadas exclusivamente pelo BFF à Software Center.
 */
public record RLoginBff(
        @Email(message = "Formato de e-mail inválido")
        @NotBlank(message = "O campo e-mail é obrigatório")
        String email,

        @JsonAlias("password")
        @NotBlank(message = "O campo senha é obrigatório")
        String senha,

        @JsonAlias({"tenantSubdomain", "tenant"})
        String tenantSubdominio
) {
}
