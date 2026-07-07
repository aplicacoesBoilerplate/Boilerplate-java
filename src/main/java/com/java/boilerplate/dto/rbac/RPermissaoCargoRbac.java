package com.java.boilerplate.dto.rbac;

import com.java.boilerplate.model.CPermissaoCargoRbac;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RPermissaoCargoRbac(
        @NotBlank(message = "O recurso da permissão é obrigatório")
        String recurso,
        @NotBlank(message = "A ação da permissão é obrigatória")
        String acao,
        @NotNull(message = "O status da permissão é obrigatório")
        Boolean liberado
) {
    public CPermissaoCargoRbac toEntity() {
        CPermissaoCargoRbac permissao = new CPermissaoCargoRbac();
        permissao.setRecurso(recurso);
        permissao.setAcao(acao);
        permissao.setLiberado(liberado);
        return permissao;
    }

    public static RPermissaoCargoRbac fromEntity(CPermissaoCargoRbac pPermissao) {
        return new RPermissaoCargoRbac(pPermissao.getRecurso(), pPermissao.getAcao(), pPermissao.getLiberado());
    }
}
