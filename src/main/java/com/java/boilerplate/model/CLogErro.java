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
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * @description Entidade para a persistência dos erros, usada para auditórias de manutenção.
 */
@Entity
@Table(name = "log_errors")
@Getter
@Setter
public class CLogErro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_error")
    private Long idError;

    @Column(name = "mensagem", nullable = false, length = 1000)
    private String mensagem;

    @Column(name = "arquivo", length = 180)
    private String arquivo;

    @Column(name = "classe", length = 220)
    private String classe;

    @Column(name = "metodo", length = 180)
    private String metodo;

    @Column(name = "linha")
    private Integer linha;

    @Column(name = "http_status_code", nullable = false)
    private Integer httpStatusCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private CUsuario usuario;

    @Column(name = "usuario_referencia", nullable = false, length = 150)
    private String usuarioReferencia = "SISTEMA";

    @CreationTimestamp
    @Column(name = "data_hora", nullable = false, updatable = false)
    private LocalDateTime dataHora;
}
