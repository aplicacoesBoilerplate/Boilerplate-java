package com.java.boilerplate.dto.preferencias;

import com.java.boilerplate.model.CPreferenciaUsuario;
import jakarta.validation.constraints.NotBlank;

/**
 * @description Contrato de preferência persistida por usuário, contexto e chave.
 * @property {String} contexto - Contexto da preferência, como global ou lista-usuarios.
 * @property {String} chave - Chave estável da preferência dentro do contexto.
 * @property {String} valorJson - Valor serializado em JSON para preservar estruturas livres do frontend.
 */
public record RPreferenciaUsuario(
        Long id,

        @NotBlank(message = "O contexto da preferência é obrigatório")
        String contexto,

        @NotBlank(message = "A chave da preferência é obrigatória")
        String chave,

        @NotBlank(message = "O valor da preferência é obrigatório")
        String valorJson) {
    public static RPreferenciaUsuario fromEntity(CPreferenciaUsuario pPreferencia) {
        return new RPreferenciaUsuario(
                pPreferencia.getIdPreferencia(),
                pPreferencia.getContexto(),
                pPreferencia.getChave(),
                pPreferencia.getValorJson());
    }
}
