package com.java.boilerplate.exception;

import com.java.boilerplate.config.RAppProperties;
import com.java.boilerplate.dto.common.RErro;
import com.java.boilerplate.model.CLogErro;
import com.java.boilerplate.model.CUsuario;
import com.java.boilerplate.repository.ILogErroRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class CErrorHandler {
    private static final ZoneId ZONE_ID_BRASIL = ZoneId.of("America/Sao_Paulo");

    private final ILogErroRepository logErroRepository;
    private final RAppProperties appProperties;

    public CErrorHandler(ILogErroRepository pLogErroRepository, RAppProperties pAppProperties) {
        this.logErroRepository = pLogErroRepository;
        this.appProperties = pAppProperties;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RErro> handlerValidationException(MethodArgumentNotValidException pException) {
        String mensagem = pException.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Dados inválidos");

        return construirResposta(pException, mensagem, HttpStatus.BAD_REQUEST, LocalDateTime.now(ZONE_ID_BRASIL), null, null);
    }

    @ExceptionHandler(CExceptionsSystem.class)
    public ResponseEntity<RErro> handlerExceptionsSystem(CExceptionsSystem pException) {
        return construirResposta(pException, pException.getMessage(), pException.getStatus(), pException.getDataHora(), pException.getCodigo(), pException.getDados());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RErro> handlerException(Exception pException) {
        return construirResposta(pException, pException.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, LocalDateTime.now(ZONE_ID_BRASIL), null, null);
    }

    private ResponseEntity<RErro> construirResposta(Exception pException, String pMensagem, HttpStatus pStatus, LocalDateTime pDataHora, String pCodigo, Map<String, Object> pDados) {
        Map<String, Object> trace = criarTrace(pException);
        salvarLog(pMensagem, pStatus, trace);

        RErro erro = new RErro(
                pMensagem,
                pDataHora,
                pStatus.value(),
                pCodigo,
                pDados,
                appProperties.deveExporTraceErro() ? trace : null
        );

        return new ResponseEntity<>(erro, pStatus);
    }

    private Map<String, Object> criarTrace(Exception pException) {
        StackTraceElement stackElement = pException.getStackTrace().length == 0
                ? null
                : pException.getStackTrace()[0];

        Map<String, Object> trace = new LinkedHashMap<>();
        trace.put("arquivo", stackElement == null ? null : stackElement.getFileName());
        trace.put("classe", stackElement == null ? null : stackElement.getClassName());
        trace.put("metodo", stackElement == null ? null : stackElement.getMethodName());
        trace.put("linha", stackElement == null ? null : stackElement.getLineNumber());
        return trace;
    }

    private void salvarLog(String pMensagem, HttpStatus pStatus, Map<String, Object> pTrace) {
        CLogErro logErro = new CLogErro();
        logErro.setMensagem(pMensagem == null ? "Erro sem mensagem" : pMensagem);
        logErro.setArquivo((String) pTrace.get("arquivo"));
        logErro.setClasse((String) pTrace.get("classe"));
        logErro.setMetodo((String) pTrace.get("metodo"));
        logErro.setLinha((Integer) pTrace.get("linha"));
        logErro.setHttpStatusCode(pStatus.value());
        preencherUsuarioResponsavel(logErro);
        logErroRepository.save(logErro);
    }

    private void preencherUsuarioResponsavel(CLogErro pLogErro) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CUsuario usuario)) {
            pLogErro.setUsuarioReferencia("SISTEMA");
            return;
        }

        pLogErro.setUsuario(usuario);
        pLogErro.setUsuarioReferencia(usuario.getEmail());
    }
}
