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
 * @description Entidade de códigos OTP para recuperações de senha dos usuários.
 */
@Entity
@Table(name = "usuarios_otp")
@Getter
@Setter
public class CUsuarioOtp {
    @Id
    @Column(name = "id_usuario")
    private Long idUsuario;

    @MapsId
    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private CUsuario usuario;

    @Column(name = "codigo_hash", nullable = false, length = 64)
    private String codigo;

    @Column(name = "tentativas", nullable = false)
    private Integer tentativas = 0;

    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;

    @Column(name = "utilizado", nullable = false)
    private Boolean utilizado = false;
}
