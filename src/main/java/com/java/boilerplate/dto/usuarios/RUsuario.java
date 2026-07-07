package com.java.boilerplate.dto.usuarios;

import com.java.boilerplate.model.CUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * @description Contrato de usuário consumido pelo boilerplate Vue.
 * @property {Long} id - Identificador único do usuário.
 * @property {String} nome - Nome exibido nas telas e filtros.
 * @property {String} email - E-mail usado para autenticação e contato.
 * @property {String} avatar - URL ou referência visual do avatar.
 * @property {String} telefone - Telefone opcional do usuário.
 * @property {Boolean} notificar - Define se o usuário aceita notificações.
 * @property {Boolean} ativo - Define se o usuário pode autenticar.
 * @property {String} papel - Papel RBAC associado ao usuário.
 */
public record RUsuario(
        Long id,
        @NotBlank(message = "O campo nome é obrigatório")
        String nome,
        @Email(message = "Formato de e-mail inválido")
        @NotBlank(message = "O campo e-mail é obrigatório")
        String email,
        String avatar,
        String telefone,
        Boolean notificar,
        Boolean ativo,
        @NotBlank(message = "O campo papel é obrigatório")
        String papel
) {
    public static RUsuario fromEntity(CUsuario pUsuario) {
        return new RUsuario(
                pUsuario.getIdUsuario(),
                pUsuario.getNome(),
                pUsuario.getEmail(),
                pUsuario.getAvatar(),
                pUsuario.getTelefone(),
                pUsuario.getNotificar(),
                pUsuario.getAtivo(),
                pUsuario.getPapel()
        );
    }
}
