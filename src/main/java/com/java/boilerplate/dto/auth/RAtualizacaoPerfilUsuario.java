package com.java.boilerplate.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @description Contrato usado pelo usuário autenticado para atualizar os dados do próprio perfil.
 * @property {String} nome - Nome exibido na aplicação.
 * @property {String} email - E-mail de contato e autenticação.
 * @property {String} avatar - URL opcional do avatar do usuário.
 * @property {String} telefone - Telefone opcional do usuário.
 * @property {Boolean} notificar - Define se o usuário aceita notificações.
 */
public record RAtualizacaoPerfilUsuario(
        @NotBlank(message = "O campo nome é obrigatório")
        @Size(max = 120, message = "O campo nome deve ter no máximo 120 caracteres")
        String nome,

        @Email(message = "Formato de e-mail inválido")
        @NotBlank(message = "O campo e-mail é obrigatório")
        @Size(max = 150, message = "O campo e-mail deve ter no máximo 150 caracteres")
        String email,

        String avatar,

        @Size(max = 30, message = "O campo telefone deve ter no máximo 30 caracteres")
        String telefone,

        Boolean notificar
) {
}
