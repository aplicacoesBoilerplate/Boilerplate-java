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

@Entity
@Table(name = "funcionalidades_cargo_rbac", uniqueConstraints = @UniqueConstraint(
        name = "uk_funcionalidade_cargo", columnNames = {"id_cargo", "funcionalidade"}
))
@Getter
@Setter
public class CFuncionalidadeCargoRbac extends CEntidadeAuditavel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_funcionalidade")
    private Long idFuncionalidade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cargo", nullable = false)
    private CCargoRbac cargo;

    @Column(name = "funcionalidade", nullable = false, length = 120)
    private String funcionalidade;

    @Column(name = "liberado", nullable = false)
    private Boolean liberado = false;
}
