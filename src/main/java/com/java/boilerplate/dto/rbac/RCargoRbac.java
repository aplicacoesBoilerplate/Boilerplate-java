package com.java.boilerplate.dto.rbac;

import com.java.boilerplate.dto.common.RAuditoriaRegistro;
import com.java.boilerplate.enums.EComportamentoPadraoPermissao;
import com.java.boilerplate.model.CCargoRbac;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * @description Contrato de cargo e permissões usado pela tela de RBAC.
 * @property {Long} id - Identificador do cargo.
 * @property {String} papel - Papel estável usado nas permissões e no vínculo com usuários.
 * @property {String} nome - Nome legível do cargo.
 * @property {String} icone - Ícone Material Design exibido no frontend.
 * @property {String} descricao - Descrição curta do cargo.
 * @property {EComportamentoPadraoPermissao} comportamentoPadrao - Regra aplicada quando não houver permissão explícita.
 * @property {List} permissoes - Permissões explicitamente configuradas.
 * @property {RRedirecionamentoInicialRbac} redirecionamentoInicial - Rota inicial do cargo.
 * @property {Boolean} ativo - Define se o cargo pode ser atribuído.
 * @property {RAuditoriaRegistro} auditoria - Metadados de criação e última atualização.
 */
public record RCargoRbac(
        Long id,

        @NotBlank(message = "O papel do cargo é obrigatório")
        String papel,

        @NotBlank(message = "O nome do cargo é obrigatório") String nome,

        @NotBlank(message = "O ícone do cargo é obrigatório")
        String icone,

        String descricao,

        @NotNull(message = "O comportamento padrão é obrigatório")
        EComportamentoPadraoPermissao comportamentoPadrao,

        @Valid List<RPermissaoCargoRbac> permissoes,
        @Valid List<RFuncionalidadeCargoRbac> funcionalidades,
        RRedirecionamentoInicialRbac redirecionamentoInicial,
        Boolean ativo,
        RAuditoriaRegistro auditoria) {
    public static RCargoRbac fromEntity(CCargoRbac pCargo) {
        return fromEntity(pCargo, RAuditoriaRegistro.fromEntity(pCargo));
    }

    public static RCargoRbac fromEntity(CCargoRbac pCargo, RAuditoriaRegistro pAuditoria) {
        return new RCargoRbac(
                pCargo.getIdCargo(),
                pCargo.getPapel(),
                pCargo.getNome(),
                pCargo.getIcone(),
                pCargo.getDescricao(),
                pCargo.getComportamentoPadrao(),
                pCargo.getPermissoes().stream()
                        .map(RPermissaoCargoRbac::fromEntity)
                        .toList(),
                pCargo.getFuncionalidades().stream()
                        .map(RFuncionalidadeCargoRbac::fromEntity)
                        .toList(),
                new RRedirecionamentoInicialRbac(
                        pCargo.getRedirecionamentoPath(), pCargo.getRedirecionamentoName(), List.of()),
                pCargo.getAtivo(),
                pAuditoria);
    }
}
