package com.java.boilerplate.dto.common;

import com.java.boilerplate.model.CEntidadeAuditavel;
import java.time.LocalDateTime;

/**
 * @description Metadados de criação e última atualização de um registro.
 */
public record RAuditoriaRegistro(
        LocalDateTime criadoEm,
        Long criadoPor,
        String criadoPorReferencia,
        LocalDateTime atualizadoEm,
        Long atualizadoPor,
        String atualizadoPorReferencia) {
    public static RAuditoriaRegistro fromEntity(CEntidadeAuditavel pEntidade) {
        return new RAuditoriaRegistro(
                pEntidade.getCriadoEm(),
                pEntidade.getCriadoPor(),
                null,
                pEntidade.getAtualizadoEm(),
                pEntidade.getAtualizadoPor(),
                null);
    }

    public RAuditoriaRegistro comReferencias(String pCriadoPorReferencia, String pAtualizadoPorReferencia) {
        return new RAuditoriaRegistro(
                criadoEm, criadoPor, pCriadoPorReferencia, atualizadoEm, atualizadoPor, pAtualizadoPorReferencia);
    }
}
