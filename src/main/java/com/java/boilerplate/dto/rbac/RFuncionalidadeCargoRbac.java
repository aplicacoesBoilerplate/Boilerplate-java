package com.java.boilerplate.dto.rbac;

import com.java.boilerplate.model.CFuncionalidadeCargoRbac;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RFuncionalidadeCargoRbac(
        @NotBlank(message = "A funcionalidade é obrigatória") String funcionalidade,
        @NotNull(message = "O status da funcionalidade é obrigatório") Boolean liberado
) {
    public CFuncionalidadeCargoRbac toEntity() {
        CFuncionalidadeCargoRbac entidade = new CFuncionalidadeCargoRbac();
        entidade.setFuncionalidade(funcionalidade);
        entidade.setLiberado(liberado);
        return entidade;
    }

    public static RFuncionalidadeCargoRbac fromEntity(CFuncionalidadeCargoRbac pFuncionalidade) {
        return new RFuncionalidadeCargoRbac(pFuncionalidade.getFuncionalidade(), pFuncionalidade.getLiberado());
    }
}
