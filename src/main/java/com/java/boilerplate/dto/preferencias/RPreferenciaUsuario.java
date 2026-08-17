package com.java.boilerplate.dto.preferencias;

import com.java.boilerplate.model.CPreferenciaUsuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @description Contrato de preferência persistida por usuário, contexto e chave.
 * @property {String} contexto - Contexto da preferência, como global ou lista-usuarios.
 * @property {String} chave - Chave estável da preferência dentro do contexto.
 * @property {String} valorJson - Valor serializado em JSON para preservar estruturas livres do frontend.
 */
public record RPreferenciaUsuario(
        Long id,
        @NotBlank(message = "O contexto da preferência é obrigatório")
        @Size(max = 120, message = "O contexto não pode exceder 120 caracteres")
        String contexto,
        @NotBlank(message = "A chave da preferência é obrigatória")
        @Size(max = 120, message = "A chave não pode exceder 120 caracteres")
        String chave,
        @NotBlank(message = "O valor da preferência é obrigatório")
        @Size(max = 16384, message = "O valor não pode exceder 16384 caracteres")
        String valorJson
) {
    public static RPreferenciaUsuario fromEntity(CPreferenciaUsuario pPreferencia) {
        return new RPreferenciaUsuario(
                pPreferencia.getIdPreferencia(),
                pPreferencia.getContexto(),
                pPreferencia.getChave(),
                pPreferencia.getValorJson()
        );
    }
}
