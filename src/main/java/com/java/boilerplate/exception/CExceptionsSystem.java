package com.java.boilerplate.exception;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

/**
 * @description Exception personalizada da aplicação para emissões de erros interceptados e persistidos.
 */
public class CExceptionsSystem extends RuntimeException {
    private final HttpStatus status;
    private final LocalDateTime dataHora;
    private final Integer retryAfterSeconds;

    public CExceptionsSystem(String pMensagem, HttpStatus pStatus) {
        super(pMensagem);
        this.status = pStatus;
        this.dataHora = LocalDateTime.now();
        this.retryAfterSeconds = null;
    }

    public CExceptionsSystem(String pMensagem, HttpStatus pStatus, Integer pRetryAfterSeconds) {
        super(pMensagem);
        this.status = pStatus;
        this.dataHora = LocalDateTime.now();
        this.retryAfterSeconds = pRetryAfterSeconds;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public Integer getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
