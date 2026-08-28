package com.java.boilerplate.dto.filtros;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @description Filtro enviado pelo motor de filtros do frontend.
 * @property {String} campo - Campo da entidade sobre o qual o filtro será aplicado.
 * @property {String} condicao - Operador selecionado pelo usuário.
 * @property {Object} valor - Valor principal do filtro.
 * @property {LocalDateTime} dataInicio - Data inicial para filtros de intervalo.
 * @property {LocalDateTime} dataFinal - Data final para filtros de intervalo.
 * @property {List} valoresSelecionados - Valores usados nos filtros de seleção e exceção.
 */
public record RFiltroConsulta(
        String campo,
        String condicao,
        Object valor,
        LocalDateTime dataInicio,
        LocalDateTime dataFinal,
        List<Object> valoresSelecionados) {}
