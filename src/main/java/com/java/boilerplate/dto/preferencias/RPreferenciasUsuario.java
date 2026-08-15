package com.java.boilerplate.dto.preferencias;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * @description Envelope usado para consultar ou salvar todas as preferências persistentes do usuário autenticado.
 * @property {List} preferencias - Lista de preferências por contexto e chave.
 */
public record RPreferenciasUsuario(
        @Valid
        @Size(max = 20, message = "O lote aceita no máximo 20 preferências")
        List<RPreferenciaUsuario> preferencias
) {
}
