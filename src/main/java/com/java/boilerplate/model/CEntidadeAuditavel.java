package com.java.boilerplate.model;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * @description Classe abstrata que possui o mapeamento de uma entidade auditável no banco de dados.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class CEntidadeAuditavel {
    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @CreatedBy
    @Column(name = "criado_por", updatable = false)
    private Long criadoPor;

    @LastModifiedDate
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @LastModifiedBy
    @Column(name = "atualizado_por")
    private Long atualizadoPor;
}
