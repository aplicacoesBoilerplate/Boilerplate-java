package com.java.boilerplate.exception;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * @description Exception personalizada da aplicação para emissões de erros interceptados e persistidos.
 */
public class CExceptionsSystem extends RuntimeException {
    private final HttpStatus status;
    private final LocalDateTime dataHora;
    private final Integer retryAfterSeconds;
    private final String codigo;
    private final Map<String, Object> dados;

    public CExceptionsSystem(String pMensagem, HttpStatus pStatus) {
        this(pMensagem, pStatus, null, null, null);
    }

    public CExceptionsSystem(String pMensagem, HttpStatus pStatus, Integer pRetryAfterSeconds) {
        this(pMensagem, pStatus, pRetryAfterSeconds, null, null);
    }

    public CExceptionsSystem(String pMensagem, HttpStatus pStatus, String pCodigo, Map<String, Object> pDados) {
        this(pMensagem, pStatus, null, pCodigo, pDados);
    }

    private CExceptionsSystem(
            String pMensagem, HttpStatus pStatus, Integer pRetryAfterSeconds, String pCodigo, Map<String, Object> pDados) {
        super(pMensagem);
        this.status = pStatus;
        this.dataHora = LocalDateTime.now();
        this.retryAfterSeconds = pRetryAfterSeconds;
        this.codigo = pCodigo;
        this.dados = pDados;
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

    public String getCodigo() {
        return codigo;
    }

    public Map<String, Object> getDados() {
        return dados;
    }
}
