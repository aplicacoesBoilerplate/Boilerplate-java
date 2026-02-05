package com.java.boilerplate.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ErrorsPersistidos {
    private Integer idError;
    private String erro;
    private Integer idSaida;
    private String arquivoError;
    private String classeError;
    private String metodoError;
    private Integer linhaError;

    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss", timezone = "America/Sao_Paulo")
    private LocalDateTime horaError;
    private Integer statusCodeError;
}
