package com.java.boilerplate.dto.errors;

import com.java.boilerplate.model.CLogErro;
import java.time.LocalDateTime;

/**
 * @description Registro de erro do sistema exposto para consulta administrativa.
 */
public record RLogErro(
        Long idError,
        String mensagem,
        String arquivo,
        String classe,
        String metodo,
        Integer linha,
        Integer httpStatusCode,
        Long idUsuario,
        String usuarioReferencia,
        LocalDateTime dataHora) {
    public static RLogErro fromEntity(CLogErro pLogErro) {
        return new RLogErro(
                pLogErro.getIdError(),
                pLogErro.getMensagem(),
                pLogErro.getArquivo(),
                pLogErro.getClasse(),
                pLogErro.getMetodo(),
                pLogErro.getLinha(),
                pLogErro.getHttpStatusCode(),
                pLogErro.getUsuario() == null ? null : pLogErro.getUsuario().getIdUsuario(),
                pLogErro.getUsuarioReferencia(),
                pLogErro.getDataHora());
    }
}
