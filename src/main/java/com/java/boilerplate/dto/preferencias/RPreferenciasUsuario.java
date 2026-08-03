package com.java.boilerplate.dto.preferencias;

import jakarta.validation.Valid;
import java.util.List;

/**
 * @description Envelope usado para consultar ou salvar todas as preferências persistentes do usuário autenticado.
 * @property {List} preferencias - Lista de preferências por contexto e chave.
 */
public record RPreferenciasUsuario(@Valid List<RPreferenciaUsuario> preferencias) {}
