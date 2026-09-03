package com.java.boilerplate.dto.rbac;

import com.java.boilerplate.model.CFuncionalidadeCargoRbac;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RFuncionalidadeCargoRbac(
        @NotBlank(message = "A funcionalidade é obrigatória")
        String funcionalidade,

        @NotNull(message = "O status da funcionalidade é obrigatório")
        Boolean liberado) {
    private static final String FUNCIONALIDADE_GERENCIAR_REGISTROS = "gerenciarRegistros";
    private static final String ALIAS_GERENCIAR_REGISTROS = "gerenciarRegistrosOutros";

    public CFuncionalidadeCargoRbac toEntity() {
        CFuncionalidadeCargoRbac entidade = new CFuncionalidadeCargoRbac();
        entidade.setFuncionalidade(funcionalidade);
        entidade.setLiberado(liberado);
        return entidade;
    }

    public static RFuncionalidadeCargoRbac fromEntity(CFuncionalidadeCargoRbac pFuncionalidade) {
        String funcionalidadeExterna = FUNCIONALIDADE_GERENCIAR_REGISTROS.equals(
                pFuncionalidade.getFuncionalidade())
                ? ALIAS_GERENCIAR_REGISTROS
                : pFuncionalidade.getFuncionalidade();
        return new RFuncionalidadeCargoRbac(funcionalidadeExterna, pFuncionalidade.getLiberado());
    }
}
