package com.java.boilerplate.exception;

import com.java.boilerplate.config.RAppProperties;
import com.java.boilerplate.dto.common.RErro;
import com.java.boilerplate.model.CLogErro;
import com.java.boilerplate.model.CUsuario;
import com.java.boilerplate.repository.ILogErroRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
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
    private static final String MENSAGEM_ERRO_INTERNO = "Erro interno ao processar a solicitação";

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
        ResponseEntity<RErro> resposta = construirResposta(pException, pException.getMessage(), pException.getStatus(), pException.getDataHora(), pException.getCodigo(), pException.getDados());
        if (pException.getStatus() == HttpStatus.TOO_MANY_REQUESTS && pException.getRetryAfterSeconds() != null) {
            HttpHeaders headers = new HttpHeaders();
            headers.putAll(resposta.getHeaders());
            headers.set(HttpHeaders.RETRY_AFTER, String.valueOf(pException.getRetryAfterSeconds()));
            return new ResponseEntity<>(resposta.getBody(), headers, resposta.getStatusCode());
        }
        return resposta;
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<RErro> handlerAuthenticationException(AuthenticationException pException) {
        return construirResposta(
                pException,
                "Credenciais inválidas",
                HttpStatus.UNAUTHORIZED,
                LocalDateTime.now(ZONE_ID_BRASIL), null, null
        );
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<RErro> handlerClientParsingException(Exception pException) {
        return construirResposta(
                pException,
                "Dados inválidos",
                HttpStatus.BAD_REQUEST,
                LocalDateTime.now(ZONE_ID_BRASIL), null, null
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RErro> handlerException(Exception pException) {
        return construirResposta(pException, MENSAGEM_ERRO_INTERNO, HttpStatus.INTERNAL_SERVER_ERROR, LocalDateTime.now(ZONE_ID_BRASIL), null, null);
    }

    private ResponseEntity<RErro> construirResposta(
            Exception pException, String pMensagem, HttpStatus pStatus, LocalDateTime pDataHora,
            String pCodigo, Map<String, Object> pDados) {
        Map<String, Object> trace = criarTrace(pException);
        if (pStatus.is5xxServerError()) {
            salvarLog(pMensagem, pStatus, trace);
        }

        RErro erro = new RErro(
                pMensagem,
                pDataHora,
                pStatus.value(), pCodigo, pDados,
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
        limitarRetencaoErros();
    }

    private void limitarRetencaoErros() {
        long excedentes = logErroRepository.count() - appProperties.limiteErrosPersistidos();
        if (excedentes <= 0) {
            return;
        }

        int tamanhoLote = (int) Math.min(excedentes, 100);
        var antigos = logErroRepository.findAll(PageRequest.of(0, tamanhoLote, Sort.by("dataHora", "idErro"))).getContent();
        if (!antigos.isEmpty()) {
            logErroRepository.deleteAllInBatch(antigos);
        }
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
