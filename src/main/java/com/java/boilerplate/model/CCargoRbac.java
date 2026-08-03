package com.java.boilerplate.model;

import com.java.boilerplate.enums.EComportamentoPadraoPermissao;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * @description Entidade que representa um cargo operacional e suas regras base de RBAC.
 */
@Entity
@Table(name = "cargos_rbac")
@Getter
@Setter
public class CCargoRbac extends CEntidadeAuditavel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cargo")
    private Long idCargo;

    @Column(name = "papel", nullable = false, unique = true, length = 80)
    private String papel;

    @Column(name = "nome", nullable = false, length = 120)
    private String nome;

    @Column(name = "icone", nullable = false, length = 80)
    private String icone;

    @Column(name = "descricao", length = 500)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "comportamento_padrao", nullable = false, length = 20)
    private EComportamentoPadraoPermissao comportamentoPadrao = EComportamentoPadraoPermissao.bloquear;

    @Column(name = "redirecionamento_path", nullable = false, length = 255)
    private String redirecionamentoPath = "/";

    @Column(name = "redirecionamento_name", length = 120)
    private String redirecionamentoName;

    @Column(name = "redirecionamento_filtros", columnDefinition = "TEXT")
    private String redirecionamentoFiltros = "[]";

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    @OrderBy("idPermissao ASC")
    @OneToMany(mappedBy = "cargo", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CPermissaoCargoRbac> permissoes = new ArrayList<>();

    @OrderBy("idFuncionalidade ASC")
    @OneToMany(mappedBy = "cargo", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CFuncionalidadeCargoRbac> funcionalidades = new ArrayList<>();

    /**
     * @description Método equivalento a um setter das permissões do cargo porém com suporte a uma lista.
     * @param pPermissoes Lista de PermissaoCargoRbac a qual o set no laço interno vai iterar.
     * @return void - O método realiza o set em lote mas não retorna nada.
     */
    public void definirPermissoes(List<CPermissaoCargoRbac> pPermissoes) {
        Map<String, CPermissaoCargoRbac> permissoesRecebidas = mapearPermissoesPorChave(pPermissoes);

        this.permissoes.removeIf(
                pPermissaoExistente -> !permissoesRecebidas.containsKey(montarChavePermissao(pPermissaoExistente)));

        if (permissoesRecebidas.isEmpty()) {
            return;
        }

        Map<String, CPermissaoCargoRbac> permissoesAtuais = mapearPermissoesPorChave(this.permissoes);

        permissoesRecebidas.forEach((pChave, pPermissaoRecebida) -> {
            CPermissaoCargoRbac permissaoExistente = permissoesAtuais.get(pChave);

            if (permissaoExistente != null) {
                permissaoExistente.setRecurso(pPermissaoRecebida.getRecurso().trim());
                permissaoExistente.setAcao(pPermissaoRecebida.getAcao().trim());
                permissaoExistente.setLiberado(Boolean.TRUE.equals(pPermissaoRecebida.getLiberado()));
                return;
            }

            pPermissaoRecebida.setCargo(this);
            this.permissoes.add(pPermissaoRecebida);
        });
    }

    public void definirFuncionalidades(List<CFuncionalidadeCargoRbac> pFuncionalidades) {
        Map<String, CFuncionalidadeCargoRbac> recebidas = new LinkedHashMap<>();
        if (pFuncionalidades != null) {
            pFuncionalidades.stream()
                    .filter(pItem -> pItem.getFuncionalidade() != null)
                    .forEach(pItem -> recebidas.put(pItem.getFuncionalidade().trim(), pItem));
        }

        this.funcionalidades.removeIf(pItem -> !recebidas.containsKey(pItem.getFuncionalidade()));
        Map<String, CFuncionalidadeCargoRbac> atuais = new LinkedHashMap<>();
        this.funcionalidades.forEach(pItem -> atuais.put(pItem.getFuncionalidade(), pItem));

        recebidas.forEach((pChave, pRecebida) -> {
            CFuncionalidadeCargoRbac existente = atuais.get(pChave);
            if (existente != null) {
                existente.setLiberado(Boolean.TRUE.equals(pRecebida.getLiberado()));
                return;
            }
            pRecebida.setFuncionalidade(pChave);
            pRecebida.setCargo(this);
            this.funcionalidades.add(pRecebida);
        });
    }

    private Map<String, CPermissaoCargoRbac> mapearPermissoesPorChave(List<CPermissaoCargoRbac> pPermissoes) {
        Map<String, CPermissaoCargoRbac> permissoesPorChave = new LinkedHashMap<>();
        if (pPermissoes == null) {
            return permissoesPorChave;
        }

        pPermissoes.stream()
                .filter(pPermissao -> pPermissao.getRecurso() != null && pPermissao.getAcao() != null)
                .forEach(pPermissao -> permissoesPorChave.put(montarChavePermissao(pPermissao), pPermissao));

        return permissoesPorChave;
    }

    private String montarChavePermissao(CPermissaoCargoRbac pPermissao) {
        return pPermissao.getRecurso().trim() + "::" + pPermissao.getAcao().trim();
    }
}
