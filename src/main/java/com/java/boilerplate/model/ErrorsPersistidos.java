package com.java.boilerplate.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "log_erros")
public class ErrorsPersistidos {
    @Id
    @Column(name = "id_error")
    private Integer idError;

    @Column(length = 255)
    private String erro;

    @Column(name = "arquivo_error", length = 150)
    private String arquivoError;

    @Column(name = "classe_error", length = 150)
    private String classeError;

    @Column(name = "metodo_error", length = 150)
    private String metodoError;

    @Column(name = "linha_error")
    private Integer linhaError;

    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss", timezone = "America/Sao_Paulo")
    @Column(name = "hora_error")
    private LocalDateTime horaError;

    @Column(name = "status_code_error")
    private Integer statusCodeError;
}
