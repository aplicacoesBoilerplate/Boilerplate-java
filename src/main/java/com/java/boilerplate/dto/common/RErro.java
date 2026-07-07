package com.java.boilerplate.dto.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * @description Contrato padrão de erro consumido pelo frontend.
 * @property {String} mensagem - Mensagem legível retornada para a interface.
 * @property {LocalDateTime} dataHora - Data e hora em que o erro foi tratado.
 * @property {Integer} httpStatusCode - Código HTTP associado ao erro.
 * @property {Map} trace - Rastro técnico exposto apenas quando habilitado por configuração.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RErro(
        String mensagem,
        @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss", timezone = "America/Sao_Paulo")
        LocalDateTime dataHora,
        Integer httpStatusCode,
        Map<String, Object> trace
) {
}
