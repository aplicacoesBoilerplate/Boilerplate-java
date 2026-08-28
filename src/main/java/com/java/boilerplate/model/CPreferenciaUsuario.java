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
 * @description Entidade que persiste preferências e filtros por usuário, contexto e chave.
 */
@Entity
@Table(
        name = "preferencias_usuario",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_preferencia_usuario_contexto_chave",
                        columnNames = {"id_usuario", "contexto", "chave"}))
@Getter
@Setter
public class CPreferenciaUsuario extends CEntidadeAuditavel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_preferencia")
    private Long idPreferencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private CUsuario usuario;

    @Column(name = "contexto", nullable = false, length = 120)
    private String contexto;

    @Column(name = "chave", nullable = false, length = 120)
    private String chave;

    @Column(name = "valor_json", nullable = false, columnDefinition = "TEXT")
    private String valorJson;
}
