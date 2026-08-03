package com.java.boilerplate.dto.filtros;

import java.util.List;

/**
 * @description Parâmetros padronizados para consultas paginadas.
 * @property {Integer} limite - Quantidade máxima de registros retornados.
 * @property {Object} proximaEntrada - Cursor usado para buscar a próxima página.
 * @property {String} ordem - Direção de ordenação ascendente ou descendente.
 * @property {List} filtros - Filtros aplicados à consulta.
 */
public record RParametrosPaginacao(
        Integer limite, Object proximaEntrada, String ordem, List<RFiltroConsulta> filtros) {}
