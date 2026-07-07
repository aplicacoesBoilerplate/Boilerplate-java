package com.java.boilerplate.dto.common;

import java.util.List;

/**
 * @description Contrato de paginação por cursor usado pelas listas genéricas do frontend.
 * @property {Integer} limite - Quantidade máxima de registros solicitados.
 * @property {Object} proximaEntrada - Cursor da próxima página, quando houver.
 * @property {List} items - Registros retornados na página atual.
 * @property {Boolean} temMaisRegistros - Define se existe próxima página disponível.
 */
public record RRespostaPaginacao<T>(
        Integer limite,
        Object proximaEntrada,
        List<T> items,
        Boolean temMaisRegistros
) {
}
