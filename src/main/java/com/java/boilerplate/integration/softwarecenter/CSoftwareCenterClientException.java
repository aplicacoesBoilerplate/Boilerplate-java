package com.java.boilerplate.integration.softwarecenter;

import org.springframework.http.HttpStatus;

/**
 * @description Falha sanitizada da Integration API, sem corpo, URI ou credenciais da requisição original.
 */
public class CSoftwareCenterClientException extends RuntimeException {
    private final HttpStatus status;

    public CSoftwareCenterClientException(HttpStatus pStatus, String pMensagem) {
        super(pMensagem);
        this.status = pStatus;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public boolean deveInvalidarSessaoLocal() {
        return status == HttpStatus.UNAUTHORIZED || status == HttpStatus.FORBIDDEN;
    }
}
