package com.java.boilerplate.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * @description Entidade que armazena o refresh token, permitindo que a autenticação seja reciclada.
 * @property {LocalDateTime} expiraEm - Determina a data de expiração do token, sendo necessário um novo login.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
public class CRefreshToken {
    @Id
    @Column(name = "id_usuario")
    private Long idUsuario;

    @MapsId
    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private CUsuario usuario;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;
}
