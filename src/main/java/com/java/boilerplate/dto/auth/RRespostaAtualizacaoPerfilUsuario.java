package com.java.boilerplate.dto.auth;

import com.java.boilerplate.dto.usuarios.RUsuario;

/**
 * @description Resposta da atualização do perfil com o usuário atualizado e um token renovado.
 * @property {RUsuario} usuario - Dados atualizados do usuário autenticado.
 * @property {String} tokenJWT - Token renovado para manter a sessão válida após alterar o e-mail.
 */
public record RRespostaAtualizacaoPerfilUsuario(RUsuario usuario, String tokenJWT) {}
