package com.java.boilerplate.dto.consulta;

import com.java.boilerplate.dto.filtros.RFiltroConsulta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import java.util.List;

/**
 * @description Contrato de consulta paginada compartilhado com o frontend.
 * @property {List} filtros - Filtros aplicados aos registros.
 * @property {String} ordenacao - Direção da ordenação do cursor.
 * @property {Integer} limite - Quantidade máxima de registros por página.
 * @property {Long} proximaEntrada - Identificador do último registro recebido na página anterior.
 * @property {Boolean} possuiMais - Estado de paginação enviado pelo cliente.
 */
public record RConsultaRegistros(
        @Valid List<RFiltroConsulta> filtros,
        @Pattern(regexp = "asc|desc", flags = Pattern.Flag.CASE_INSENSITIVE, message = "A ordenação deve ser asc ou desc")
        String ordenacao,
        @Min(value = 1, message = "O limite deve ser maior que zero")
        Integer limite,
        Long proximaEntrada,
        Boolean possuiMais
) {
}
