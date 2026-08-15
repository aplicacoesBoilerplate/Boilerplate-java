package com.java.boilerplate.service;

import com.java.boilerplate.exception.CExceptionsSystem;
import com.java.boilerplate.model.CUsuario;
import com.java.boilerplate.service.helpers.COtpService;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class CRecoveryService {
    private final CUsuarioService usuarioService;
    private final COtpService otpService;

    public CRecoveryService(CUsuarioService pUsuarioService, COtpService pOtpService) {
        this.usuarioService = pUsuarioService;
        this.otpService = pOtpService;
    }

    @Async("securityTaskExecutor")
    public void solicitar(String pEmail) {
        try {
            CUsuario usuario = usuarioService.buscarEntidadePorEmail(pEmail);
            otpService.gerarCodigo(usuario);
        } catch (CExceptionsSystem pException) {
            if (pException.getStatus() != HttpStatus.NOT_FOUND) {
                throw pException;
            }
        }
    }
}
