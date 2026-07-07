package com.java.boilerplate.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * @description Entidade de permissão explícita aplicada a um cargo para um recurso e ação específicos.
 * @property {CCargoRbac} cargo - Relacionamento com o cargo vinculado a um conjunto de regras RBAC.
 */
@Entity
@Table(
        name = "permissoes_cargo_rbac",
        uniqueConstraints = @UniqueConstraint(name = "uk_permissao_cargo_recurso_acao", columnNames = {"id_cargo", "recurso", "acao"})
)
@Getter
@Setter
public class CPermissaoCargoRbac extends CEntidadeAuditavel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_permissao")
    private Long idPermissao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cargo", nullable = false)
    private CCargoRbac cargo;

    @Column(name = "recurso", nullable = false, length = 120)
    private String recurso;

    @Column(name = "acao", nullable = false, length = 120)
    private String acao;

    @Column(name = "liberado", nullable = false)
    private Boolean liberado = false;
}
