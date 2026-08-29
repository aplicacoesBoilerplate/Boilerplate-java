package com.java.boilerplate.dto.consulta;

import com.java.boilerplate.dto.filtros.RFiltroConsulta;

import java.util.List;

/**
 * @description Resposta de consulta paginada compatível com a interface IRespostaConsultaRegistros do frontend.
 * @template TRegistro - Tipo do registro retornado na página.
 * @property {List} filtros - Filtros normalizados utilizados na consulta.
 * @property {String} ordenacao - Direção de ordenação aplicada.
 * @property {Integer} limite - Limite aplicado à página.
 * @property {Long} proximaEntrada - Identificador para consulta da página seguinte.
 * @property {Boolean} possuiMais - Indica se existe uma página seguinte.
 * @property {List} registros - Registros retornados na página atual.
 */
public record RRespostaConsultaRegistros<TRegistro>(
        List<RFiltroConsulta> filtros,
        String ordenacao,
        Integer limite,
        Long proximaEntrada,
        Boolean possuiMais,
        List<TRegistro> registros
) {
}
