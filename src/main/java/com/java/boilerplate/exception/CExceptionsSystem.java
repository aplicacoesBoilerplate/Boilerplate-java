package com.java.boilerplate.exception;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

/**
 * @description Exception personalizada da aplicação para emissões de erros interceptados e persistidos.
 */
public class CExceptionsSystem extends RuntimeException {
    private final HttpStatus status;
    private final LocalDateTime dataHora;

    public CExceptionsSystem(String pMensagem, HttpStatus pStatus) {
        super(pMensagem);
        this.status = pStatus;
        this.dataHora = LocalDateTime.now();
    }

    public HttpStatus getStatus() {
        return status;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }
}
