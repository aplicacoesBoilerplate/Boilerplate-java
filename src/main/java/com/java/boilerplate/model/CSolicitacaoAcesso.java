package com.java.boilerplate.model;

import com.java.boilerplate.enums.EStatusSolicitacaoAcesso;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * @description Entidade das solicitações de acesso (registro de contas).
 */
@Entity
@Table(name = "solicitacoes_acesso")
@Getter
@Setter
public class CSolicitacaoAcesso extends CEntidadeAuditavel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitacao")
    private Long idSolicitacao;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false, unique = true)
    private CUsuario usuario;

    @Column(name = "liberado", nullable = false)
    private Boolean liberado = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EStatusSolicitacaoAcesso status = EStatusSolicitacaoAcesso.PENDENTE;
}
